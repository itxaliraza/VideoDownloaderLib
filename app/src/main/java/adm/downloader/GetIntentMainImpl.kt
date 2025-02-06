package adm.downloader

import android.content.Context
import android.content.Intent
import com.down.adm_core.MainActivity
import com.example.domain.managers.get_intent.GetIntentMain

class GetIntentMainImpl(private val context: Context): GetIntentMain {
    override fun getMainIntent(): Intent {
        return Intent(context, MainActivity::class.java)
    }
}