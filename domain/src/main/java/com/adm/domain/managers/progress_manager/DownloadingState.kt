package com.adm.domain.managers.progress_manager

import com.adm.domain.R

enum class DownloadingState {
    Idle,
    Progress,
    PausedNetwork,
    Failed,
    Paused,
    Success
}

fun DownloadingState.getIcon(): Int {
   return when (this){
        DownloadingState.Idle ->{
            com.adm.framework.R.drawable.ic_play

        }
        DownloadingState.Progress ->{
            com.adm.framework.R.drawable.ic_pause

        }
        DownloadingState.PausedNetwork ->{
            com.adm.framework.R.drawable.ic_no_wifi

        }
        DownloadingState.Failed ->{
            com.adm.framework.R.drawable.ic_restart
        }
        DownloadingState.Paused ->{
            com.adm.framework.R.drawable.ic_play

        }
        DownloadingState.Success ->{
            com.adm.framework.R.drawable.ic_play

        }
    }
}
fun DownloadingState.getIconColor(): Int? {
   return when (this){
        DownloadingState.Idle ->{
            com.adm.framework.R.color.mainGreen

        }
        DownloadingState.Progress ->{
            com.adm.framework.R.color. dark_gray

        }
        DownloadingState.PausedNetwork ->{
           null

        }
        DownloadingState.Failed ->{
            null
        }
        DownloadingState.Paused ->{
            com.adm.framework.R.color. mainGreen

        }
        DownloadingState.Success ->{
            com.adm.framework.R.color.mainGreen

        }
    }
}
fun DownloadingState.getStatusColor(): Int {
   return when (this){
        DownloadingState.Idle ->{
            com.adm.framework.R.color.mainGreen

        }
        DownloadingState.Progress ->{
            com.adm.framework.R.color. mainGreen

        }
        DownloadingState.PausedNetwork ->{
            com.adm.framework.R.color.orangeGradient

        }
        DownloadingState.Failed ->{
            com.adm.framework.R.color.redGradient
        }
        DownloadingState.Paused ->{
            com.adm.framework.R.color.orangeGradient

        }
        DownloadingState.Success ->{
            com.adm.framework.R.color.mainGreen

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