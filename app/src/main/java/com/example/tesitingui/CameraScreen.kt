package com.example.tesitingui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch


@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    CameraContent(
        viewModel = viewModel,
        onPhotoCaptured = viewModel::onPhotoCaptured
    )
    viewModel.state.capturedImage?.let { capturedImage: Bitmap ->
//        viewModel.state.currentImage?.let { imageAnalyzer.analyze(it) }
        CapturedImageBitmapDialog(
            capturedImage = capturedImage,
            onDismissRequest = viewModel::onCapturedPhotoConsumed,
            viewModel
        )
        viewModel.state = viewModel.state.copy(capturingInProgress = false)
        Log.d("camLog", "done")
    }
}

@Composable
private fun CapturedImageBitmapDialog(
    capturedImage: Bitmap,
    onDismissRequest: () -> Unit,
    viewModel: CameraViewModel
) {
    val capturedImageBitmap: ImageBitmap = remember { capturedImage.asImageBitmap() }
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = capturedImageBitmap,
                contentDescription = "Captured photo"
            )
            Log.d("camLog", viewModel.state.imageText)
            SelectionContainer() {
                Text(text = viewModel.state.imageText, fontSize = 15.sp, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(15.dp))
            }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
private fun CameraContent(
    viewModel: CameraViewModel,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    val scope = rememberCoroutineScope()
    Scaffold(
        backgroundColor = androidx.compose.ui.graphics.Color.Black,
        drawerBackgroundColor = androidx.compose.ui.graphics.Color.Black,
    ) {
        Column {
            val config = LocalConfiguration.current
            val screenWidth = config.screenWidthDp
            val viewHeight = screenWidth/3*4
            Card(modifier = Modifier
                .padding(top = 100.dp)
                .height(viewHeight.dp)
                .width((screenWidth + 1).dp)) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize(),
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            setBackgroundColor(Color.BLACK)
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_START
                        }.also { previewView ->
                            previewView.controller = cameraController
                            cameraController.bindToLifecycle(lifecycleOwner)
                        }
                    }
                )
            }
            if(viewModel.state.capturingInProgress){
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(100.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(bottom = 50.dp)
                    .fillMaxWidth()) {
                Button(
                    colors = ButtonDefaults.buttonColors(backgroundColor = androidx.compose.ui.graphics.Color.White),
                    elevation = ButtonDefaults.elevation(10.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(70.dp),
                    onClick = {
                        scope.launch {
                            viewModel.state = viewModel.state.copy(capturingInProgress = true)
                            Log.d("camLog", "capturing")
                            val mainExecutor = ContextCompat.getMainExecutor(context)

                            cameraController.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    viewModel.analyze(image)
                                    val correctedBitmap: Bitmap = image
                                        .toBitmap()
                                        .rotateBitmap(image.imageInfo.rotationDegrees)
                                    onPhotoCaptured(correctedBitmap)
                                    image.close()
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraContent", "Error capturing image", exception)
                                }
                            })
                        }
                    }) {}
            }
        }
    }
}