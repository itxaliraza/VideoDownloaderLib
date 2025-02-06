package adm.downloader.commonui

import adm.downloader.R
import com.adm.core.components.DownloadingState


/*
enum class DownloadingState {
    Idle,
    Progress,
    PausedNetwork,
    Failed,
    Paused,
    Success
}
*/

fun DownloadingState.getIcon(): Int {
    return when (this){
        DownloadingState.Idle ->{
            R.drawable.ic_play

        }
        DownloadingState.Progress ->{
            R.drawable.ic_pause

        }
        DownloadingState.PausedNetwork ->{
            R.drawable.ic_no_wifi

        }
        DownloadingState.Failed ->{
            R.drawable.ic_restart
        }
        DownloadingState.Paused ->{
            R.drawable.ic_play

        }
        DownloadingState.Success ->{
            R.drawable.ic_play

        }
    }
}

fun DownloadingState.getName(): String {
    if (this==DownloadingState.PausedNetwork){
        return "No Internet"
    }
    else
        return this.name
}

fun String.getDownloadingStatus(): DownloadingState {
    return DownloadingState.entries.firstOrNull { it.name == this } ?: DownloadingState.Idle
}