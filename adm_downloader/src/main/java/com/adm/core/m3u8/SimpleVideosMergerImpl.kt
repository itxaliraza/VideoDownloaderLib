package com.adm.core.m3u8

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.adm.core.services.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class SimpleVideosMergerImpl(private val logger: Logger) : VideosMerger {
    companion object {
        const val TAG = "SimpleVideosMergerImpl"
        private const val BUFFER_SIZE = 1024 * 1024 // 1 MB
    }

    /**
     * Merges fragmented MP4 (.m4s) segments into a standard MP4 file.
     * Uses MediaMuxer to properly remux all segments with correct timestamps.
     */
    override suspend fun mergeFragmentedMp4Segments(
        folderPath: String,
        destPath: String,
        sharedInitFile: File?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        val allFiles = folder.listFiles() ?: emptyArray()

        // Get only media segments (.m4s files), sorted by index
        val mediaSegments = allFiles
            .filter { it.extension == "m4s" }
            .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }

        if (mediaSegments.isEmpty()) {
            logger.logError(TAG, "No .m4s segment files found to merge")
            return@withContext Result.failure(Exception("No segment files"))
        }

        logger.logMessage(TAG, "Found ${mediaSegments.size} media segments to merge")
        if (sharedInitFile != null) {
            logger.logMessage(TAG, "Using shared init segment: ${sharedInitFile.name} (${sharedInitFile.length()} bytes)")
        }

        val outputFile = File(destPath)
        outputFile.createParentFileIfNotExists()
        if (outputFile.exists()) outputFile.delete()

        var muxer: MediaMuxer? = null
        val trackIndexMap = mutableMapOf<String, Int>() // "video" or "audio" -> muxerTrackIndex
        var muxerStarted = false

        // Track cumulative duration for each track type to maintain timeline continuity
        val cumulativeDurationUs = mutableMapOf<String, Long>()

        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            mediaSegments.forEachIndexed { segmentIndex, segmentFile ->
                logger.logMessage(TAG, "Processing segment $segmentIndex: ${segmentFile.name}")

                // Combine init + segment into a temporary complete fragment
                val combinedFile = File(folder, "temp_combined_$segmentIndex.mp4")
                if (sharedInitFile != null && sharedInitFile.exists()) {
                    combineInitAndSegment(sharedInitFile, segmentFile, combinedFile)
                } else {
                    // No init file - use segment directly (less ideal but may work for some streams)
                    segmentFile.copyTo(combinedFile, overwrite = true)
                }

                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(combinedFile.absolutePath)
                    val trackCount = extractor.trackCount

                    if (trackCount == 0) {
                        logger.logError(TAG, "No tracks found in segment $segmentIndex")
                        combinedFile.delete()
                        return@withContext Result.failure(Exception("No tracks in segment $segmentIndex"))
                    }

                    // On first segment, discover and add all tracks to muxer
                    if (segmentIndex == 0) {
                        for (trackIndex in 0 until trackCount) {
                            val format = extractor.getTrackFormat(trackIndex)
                            val mime = format.getString(MediaFormat.KEY_MIME) ?: "unknown"

                            val trackType = when {
                                mime.startsWith("video/") -> "video"
                                mime.startsWith("audio/") -> "audio"
                                else -> "track_$trackIndex"
                            }

                            val muxerTrackIndex = muxer.addTrack(format)
                            trackIndexMap[trackType] = muxerTrackIndex
                            cumulativeDurationUs[trackType] = 0L

                            logger.logMessage(TAG, "Added $trackType track (mime: $mime) -> muxer track $muxerTrackIndex")
                        }
                        muxer.start()
                        muxerStarted = true
                        logger.logMessage(TAG, "MediaMuxer started with ${trackIndexMap.size} tracks")
                    }

                    // Process each track and write samples with adjusted timestamps
                    for (trackIndex in 0 until trackCount) {
                        val format = extractor.getTrackFormat(trackIndex)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: "unknown"
                        val trackType = when {
                            mime.startsWith("video/") -> "video"
                            mime.startsWith("audio/") -> "audio"
                            else -> "track_$trackIndex"
                        }

                        val muxerTrackIndex = trackIndexMap[trackType]
                        if (muxerTrackIndex == null) {
                            logger.logMessage(TAG, "Skipping unknown track type: $trackType")
                            continue
                        }

                        extractor.selectTrack(trackIndex)
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
                        val bufferInfo = MediaCodec.BufferInfo()
                        val baseTimeUs = cumulativeDurationUs[trackType] ?: 0L

                        var sampleCount = 0
                        var maxPts = 0L

                        while (true) {
                            buffer.clear()
                            bufferInfo.offset = 0
                            bufferInfo.size = extractor.readSampleData(buffer, 0)

                            if (bufferInfo.size < 0) break

                            val originalPts = extractor.sampleTime
                            // Adjust timestamp by adding cumulative duration from previous segments
                            bufferInfo.presentationTimeUs = baseTimeUs + originalPts
                            bufferInfo.flags = extractor.sampleFlags

                            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                            sampleCount++

                            // Track the maximum presentation time in this segment
                            if (originalPts > maxPts) {
                                maxPts = originalPts
                            }

                            if (!extractor.advance()) break
                        }

                        logger.logMessage(TAG, "Wrote $sampleCount samples from $trackType track of segment $segmentIndex")

                        // Update cumulative duration for this track based on the last sample's timestamp
                        // Add a small buffer to account for frame duration
                        if (maxPts > 0) {
                            cumulativeDurationUs[trackType] = baseTimeUs + maxPts + 40000 // +40ms buffer
                        }

                        extractor.unselectTrack(trackIndex)
                    }

                } finally {
                    extractor.release()
                    combinedFile.delete()
                }
            }

            if (muxerStarted) {
                muxer.stop()
            }

            // Cleanup: Delete ALL temp files including init segment and media segments
            sharedInitFile?.let {
                if (it.exists()) {
                    it.delete()
                    logger.logMessage(TAG, "Deleted shared init segment")
                }
            }

            mediaSegments.forEach {
                if (it.exists()) {
                    it.delete()
                }
            }

            // Delete any remaining temp files
            folder.listFiles()?.forEach {
                if (it.name.startsWith("temp_") || it.name.startsWith("init_")) {
                    it.delete()
                }
            }

            folder.deleteRecursively()

            logger.logMessage(TAG, "✅ Successfully merged ${mediaSegments.size} fragments into: ${outputFile.absolutePath}")
            logger.logMessage(TAG, "Output file size: ${outputFile.length()} bytes")

            return@withContext Result.success(true)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.logError(TAG, "❌ Error merging fragmented MP4 segments: ${e.message}")
            e.printStackTrace()
            return@withContext Result.failure(e)
        } finally {
            try {
                muxer?.release()
            } catch (e: Exception) {
                logger.logError(TAG, "Error releasing muxer: ${e.message}")
            }
        }
    }

    /**
     * Combines init segment and media segment into a single temporary file.
     */
    private fun combineInitAndSegment(initFile: File, segmentFile: File, outputFile: File) {
        if (outputFile.exists()) outputFile.delete()

        FileOutputStream(outputFile).use { output ->
            // Write init segment first
            FileInputStream(initFile).use { input ->
                input.copyTo(output)
            }

            // Write media segment
            FileInputStream(segmentFile).use { input ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Legacy merge method for TS segments (simple binary concatenation).
     */
    override suspend fun mergeVideos(folderPath: String, destPath: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val file = File(folderPath)
            val list = (file.listFiles()?.toList()?.mapNotNull { it } ?: emptyList())
            val sorted = list.sortedBy {
                it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE
            }

            val outputFile = File(destPath)
            outputFile.createParentFileIfNotExists()
            if (outputFile.exists()) outputFile.delete()
            outputFile.createNewFile()

            try {
                FileOutputStream(outputFile).use { outputStream ->
                    for (segmentFile in sorted) {
                        if (!segmentFile.exists()) {
                            logger.logMessage(TAG, "Segment file not found: ${segmentFile.path}")
                            continue
                        }
                        FileInputStream(segmentFile).use { inputStream ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }

                try {
                    file.listFiles()?.forEach { it.delete() }
                    file.delete()
                    logger.logMessage(TAG, "✅ Merged TS file created at: $outputFile")
                    return@withContext Result.success(true)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.logError(TAG, "Failed to delete temp folder: ${file.absolutePath}")
                    return@withContext Result.failure(e)
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                logger.logError(TAG, "Error merging TS files: ${e.message}")
                return@withContext Result.failure(e)
            }
        }
    }

}

fun File.createParentFileIfNotExists() {
    if (this.parentFile?.exists()?.not() == true)
        this.parentFile?.mkdirs()
}

fun File.createNewFileIfNotExists() {
    Log.d(
        "createNewFileIfNotExists",
        "Chunk destFile path=${this.path} exists= ${this.exists()}"
    )
    if (this.exists().not()) {
        val pth = this.createNewFile()
        Log.d("createNewFileIfNotExists", "Chunk pth= $pth")
    }
}

fun String.createParentFileIfNotExists() {
    File(this).createParentFileIfNotExists()
}

fun File.createThisFolderIfNotExists() {
    if (this.exists().not())
        this.mkdirs()
}
