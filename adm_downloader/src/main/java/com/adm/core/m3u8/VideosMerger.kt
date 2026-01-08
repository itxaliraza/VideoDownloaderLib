package com.adm.core.m3u8
import java.io.File

interface VideosMerger {
    /**
     * Merges TS video segments using simple binary concatenation.
     * @param folderPath Path to folder containing numbered TS segments
     * @param destPath Destination path for merged output file
     * @return Result indicating success or failure
     */
    suspend fun mergeVideos(folderPath: String, destPath: String): Result<Boolean>

    /**
     * Merges fragmented MP4 (.m4s) segments into a standard MP4 file.
     * This method properly handles the shared init segment and uses MediaMuxer for correct remuxing.
     * @param folderPath Path to folder containing numbered .m4s segments
     * @param destPath Destination path for merged output file
     * @param sharedInitFile The single shared init segment file used by all media segments (optional)
     * @return Result indicating success or failure
     */
    suspend fun mergeFragmentedMp4Segments(
        folderPath: String,
        destPath: String,
        sharedInitFile: File? = null,
    ): Result<Boolean>
    
}
