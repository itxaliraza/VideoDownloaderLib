package com.example.domain.managers.progress_manager


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
            com.example.framework.R.drawable.ic_play

        }
        DownloadingState.Progress ->{
             com.example.framework.R.drawable.ic_pause

        }
        DownloadingState.PausedNetwork ->{
             com.example.framework.R.drawable.ic_no_wifi

        }
        DownloadingState.Failed ->{
             com.example.framework.R.drawable.ic_restart
        }
        DownloadingState.Paused ->{
             com.example.framework.R.drawable.ic_play

        }
        DownloadingState.Success ->{
             com.example.framework.R.drawable.ic_play

        }
    }
}
fun DownloadingState.getIconColor(): Int? {
   return when (this){
        DownloadingState.Idle ->{
             com.example.framework.R.color.mainGreen

        }
        DownloadingState.Progress ->{
             com.example.framework.R.color. dark_gray

        }
        DownloadingState.PausedNetwork ->{
           null

        }
        DownloadingState.Failed ->{
            null
        }
        DownloadingState.Paused ->{
             com.example.framework.R.color. mainGreen

        }
        DownloadingState.Success ->{
             com.example.framework.R.color.mainGreen

        }
    }
}
fun DownloadingState.getStatusColor(): Int {
   return when (this){
        DownloadingState.Idle ->{
             com.example.framework.R.color.mainGreen

        }
        DownloadingState.Progress ->{
             com.example.framework.R.color. mainGreen

        }
        DownloadingState.PausedNetwork ->{
             com.example.framework.R.color.orangeGradient

        }
        DownloadingState.Failed ->{
             com.example.framework.R.color.redGradient
        }
        DownloadingState.Paused ->{
             com.example.framework.R.color.orangeGradient

        }
        DownloadingState.Success ->{
             com.example.framework.R.color.mainGreen

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