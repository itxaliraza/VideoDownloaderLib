package com.down.adm_core

import adm.downloader.commonui.HorizontalSpace
import adm.downloader.commonui.VerticalSpace
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adm.core.services.logger.logsss
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.domain.managers.progress_manager.DownloadingState
import com.example.domain.managers.progress_manager.InProgressVideoUi
import com.example.domain.managers.progress_manager.getIcon
import com.example.domain.managers.progress_manager.getIconColor
import com.example.domain.managers.progress_manager.getName
import com.example.domain.managers.progress_manager.getStatusColor
import com.example.framework.core.Commons.formatSizeToMbs
import ir.kaaveh.sdpcompose.sdp
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progressVideos by viewModel.videos.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var textUrl by remember {
            mutableStateOf("https://www.sample-videos.com/video321/mp4/720/big_buck_bunny_720p_30mb.mp4")
        }
        var fileName by remember {
            mutableStateOf(System.currentTimeMillis().toString())
        }
        Text("Main Screen")
        VerticalSpacer()
        TextField(
            value = fileName,
            onValueChange = {
                fileName = it
            },
            modifier = Modifier
                .fillMaxWidth()
        )
        TextField(
            value = textUrl,
            onValueChange = {
                textUrl = it
            },
            modifier = Modifier
                .fillMaxWidth()
        )
        Button(onClick = {
            viewModel.download(context, fileName, textUrl)
        }) {
            Text("Download")
        }

        Button(onClick = {
            val file= File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"alii/text/${System.currentTimeMillis()}.txt")
            file.parentFile?.mkdirs()
             file.writeText(logsss)

        }) {
            Text("Lpgss")
        }


        LazyColumn {
            items(progressVideos) {
                InProgressVideoItem(
                    inProgressVideoUi = it,
                    playPauseClick = {
                        viewModel.playPauseVideo(it)

                    },
                    browseVideoClick = {
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun InProgressVideoItem(
    modifier: Modifier = Modifier,
    inProgressVideoUi: InProgressVideoUi,
    browseVideoClick: (InProgressVideoUi) -> Unit,
    playPauseClick: (InProgressVideoUi) -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = inProgressVideoUi.thumb,
                contentDescription = null,
                modifier = modifier
                    .width(70.sdp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            HorizontalSpace()
            Column {
                VerticalSpace(4)
                Text(
                    text = inProgressVideoUi.fileName,
                    fontSize = 14.sp,
                )

                VerticalSpace()
                LinearProgressIndicator(
                    progress = { if (inProgressVideoUi.progress.isNaN()) 0f else inProgressVideoUi.progress },
                )
                VerticalSpace(4)
                Text(
                    text = inProgressVideoUi.status.getName(),
                    color = colorResource(inProgressVideoUi.status.getStatusColor()),
                )
                VerticalSpace(5)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (inProgressVideoUi.url.contains(".m3u8")) {
                        Text(
                            text = "${inProgressVideoUi.downloadedSize}/${
                                inProgressVideoUi.totalSize
                            }",
                        )
                    } else {
                        Text(
                            text = "${inProgressVideoUi.downloadedSize.formatSizeToMbs(context)}/${
                                inProgressVideoUi.totalSize.formatSizeToMbs(
                                    context
                                )
                            }",
                        )
                    }


                    Row {

                        /* MyIconButton(
                             modifier = Modifier.size(29.dp),
                             icon = com.adm.framework.R.drawable.browse,
                             onIconClick = {
                                 browseVideoClick(inProgressVideoUi)
                             },
                             tint = colorResource(com.adm.framework.R.color.mainGreen)
                         )
                         HorizontalSpace()*/

                        if (inProgressVideoUi.status != DownloadingState.Success) {
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    playPauseClick(inProgressVideoUi)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(inProgressVideoUi.status.getIcon()),
                                    tint = inProgressVideoUi.status.getIconColor()
                                        ?.let { colorResource(it) }
                                        ?: Color.Unspecified,
                                    contentDescription = null
                                )
                            }
                        }

                        HorizontalSpace()
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalSpacer(height: Int = 10) {
    Modifier.height(height.sdp)
}