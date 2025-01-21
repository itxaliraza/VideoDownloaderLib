package com.adm.core.m3u8

import android.util.Log
import com.adm.core.services.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SimpleVideosMergerImpl(private val logger: Logger) : VideosMerger {
    companion object {
        const val TAG = "SimpleVideosMergerImpl"
    }

    override suspend fun mergeVideos(folderPath: String, destPath: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val file = File(folderPath)
            val list = (file.listFiles()?.toList()?.mapNotNull { it } ?: emptyList())
            val sorted = list.sortedBy {
                it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE
            }
            logger.logMessage(
                TAG,
                "Merge Videos (folderexists=${file.exists()})\nfolder Path=${folderPath}\nItems=${list.size}\nList=${sorted} \ndestFile= ${destPath}"
            )
            val outputFile = File(destPath)
            logger.logMessage(TAG,"outputFile")

            outputFile.createParentFileIfNotExists()
            logger.logMessage(TAG,"createParentFileIfNotExists")

            outputFile.createNewFile()
            logger.logMessage(TAG,"createNewFile")
            try {
                FileOutputStream(outputFile).use { outputStream ->
                    for (segmentFile in sorted) {
                        if (!segmentFile.exists()) {
                            logger.logMessage(TAG, "Segment file not found: ${segmentFile.path}")
                            continue
                        }
                        FileInputStream(segmentFile).use { inputStream ->
                            val buffer = ByteArray(1024 * 1024) // 1 MB buffer
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }

                try {
                    file.listFiles()?.forEach {
                        it.delete()
                    }
                    file.delete()
                    logger.logMessage(TAG, "Merged TS file created at: $outputFile")
                    return@withContext Result.success(true)
                } catch (e: Exception) {
                    if (e is CancellationException)
                        throw e
                    logger.logError(TAG, "Failed to delete temp folder: ${file.absolutePath}")
                    return@withContext Result.failure(e)
                }

            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
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
    if (this.exists().not()){

       val pth=this.createNewFile()
        Log.d(
            "createNewFileIfNotExists",
            "Chunk pth= $pth"
        )
    }

}

fun String.createParentFileIfNotExists() {
    File(this).createParentFileIfNotExists()
}

fun String.createThisFolderIfNotExists() {
   File(this).createThisFolderIfNotExists()
}
fun File.createThisFolderIfNotExists() {
     if (this.exists().not())
        this.mkdirs()
}