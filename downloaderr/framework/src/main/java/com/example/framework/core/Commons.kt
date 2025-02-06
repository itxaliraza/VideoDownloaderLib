package com.example.framework.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.core.content.ContextCompat
import com.example.framework.R
import java.net.URLEncoder
import java.util.Locale

object Commons {

    fun String.capitalizeIt(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }
    fun String.rawTextToWebUrl(enginePreFixUrl: String): String {
        return if (isAUrl()) {
            this
        } else {
            enginePreFixUrl + this.encodeTextToUrl()
        }
    }

    fun String.encodeTextToUrl() = URLEncoder.encode(this, "UTF-8")

    fun String.isAUrl(): Boolean {
        return startsWith("http") || contains("www.") || contains(".com") || contains(".pk")
    }

    fun Long.formatSizeToMbs(context: Context): String {
        return Formatter.formatFileSize(context, this)
    }
    fun Long.formatDuration(): String {
        val seconds = this / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format("%02d:%02d", minutes % 60, seconds % 60)
        }
    }

    fun shareApp(context: Context) {
        val appPackageName = context.packageName // Your app's package name
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$appPackageName"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey, I found this amazing app! Check it out: $playStoreUrl"
            )
        }

        // Use a chooser to let the user select the sharing app
        context.startActivity(Intent.createChooser(shareIntent, "Share app via"))
    }


    fun composeEmail(context: Context, message: String, callBack: (Boolean) -> Unit) {
        val deviceName = Build.MODEL
        val deviceMan = Build.MANUFACTURER
        val oS = Build.VERSION.SDK_INT
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("commonaiapps@gmail.com"))
        intent.putExtra(
            Intent.EXTRA_SUBJECT, ContextCompat.getString(
                context, R.string.app_name
            ) + " " + ContextCompat.getString(
                context,
                R.string.feedback
            ) + "\nDevice Model-$deviceMan $deviceName\nOS Version-$oS"
        )
        intent.putExtra(Intent.EXTRA_TEXT, message)
        if (intent.resolveActivity(context.packageManager) != null) {
            callBack(false)
            context.startActivity(intent)
        } else {
            callBack.invoke(true)
        }
    }

}