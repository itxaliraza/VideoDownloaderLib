package com.example.domain

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log

class ScanFileUseCase {

    operator fun invoke(context: Context, filePath: String, mimeType:String) {

        Log.d("ScanFileUseCase","File path = $filePath, mimetype= $mimeType")
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            arrayOf(mimeType),
            { path, uri -> }
        )
    }
}