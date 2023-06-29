package com.example.tesitingui

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class CameraViewModel : ViewModel(), ImageAnalysis.Analyzer {

    var state by mutableStateOf(CameraState())

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun  analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API
            // ...
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    state = state.copy(imageText = visionText.text)
                    // Task completed successfully
                    // ...
                }
                .addOnFailureListener { e ->
                    Log.e("camLog", e.message.toString())
                    Log.e("camLog", "error")
                    // Task failed with an exception
                    // ...
                }
        }
    }



    fun onPhotoCaptured(bitmap: Bitmap) {
        // TODO: Process your photo, for example store it in the MediaStore
        // here we only do a dummy showcase implementation
        updateCapturedPhotoState(bitmap)
    }

    fun onCapturedPhotoConsumed() {
        updateCapturedPhotoState(null)
    }

    private fun updateCapturedPhotoState(updatedPhoto: Bitmap?) {
        state.capturedImage?.recycle()
        state = state.copy(capturedImage = updatedPhoto)
    }

    override fun onCleared() {
        state.capturedImage?.recycle()
        super.onCleared()
    }

}