package com.adm.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrisHelper {
    suspend fun  getURi(filePath:String,context: Context): Uri? {
        val cr = context.contentResolver
        if (filePath.endsWith("mp4")) {
            return filePath.getVideoActualUri(cr)
        } else if (filePath.endsWith("mp3"))
            return filePath.getAudioActualUri(cr)
        return filePath.getImageActualUri(cr)

    }

    suspend fun String.getVideoActualUri(contentResolver: ContentResolver): Uri? {
        Log.d("cvv", "getVideoActualUri:$this")
        return withContext(Dispatchers.IO) {
            val uri = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }

                else -> {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            }
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Video.Media._ID),
                "${MediaStore.Video.Media.DATA} = ?",
                arrayOf(this@getVideoActualUri),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    contentUri.buildUpon().appendPath(id.toString()).build()
                } else {
                    null
                }
            }
        }
    }

    private suspend fun String.getImageActualUri(contentResolver: ContentResolver): Uri? {
        return withContext(Dispatchers.IO) {
            val uri = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }

                else -> {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DATA} = ?",
                arrayOf(this@getImageActualUri),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    contentUri.buildUpon().appendPath(id.toString()).build()
                } else {
                    null
                }
            }
        }
    }

    suspend fun String.getAudioActualUri(contentResolver: ContentResolver): Uri? {
        return withContext(Dispatchers.IO) {
            val uri = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }

                else -> {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            }
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.DATA} = ?",
                arrayOf(this@getAudioActualUri),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    contentUri.buildUpon().appendPath(id.toString()).build()
                } else {
                    null
                }
            }
        }
    }

}