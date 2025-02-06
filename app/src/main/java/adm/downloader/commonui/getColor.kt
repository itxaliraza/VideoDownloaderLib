package adm.downloader.commonui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import ir.kaaveh.sdpcompose.sdp

@Composable
fun Int.getColor() = colorResource(id = this)

@Composable
fun VerticalSpace(height: Int = 10) {
    Spacer(
        modifier = Modifier
            .height(height.sdp)
    )
}

@Composable
fun HorizontalSpace(width: Int = 10) {
    Spacer(
        modifier = Modifier
            .width(width.sdp)
    )
}
