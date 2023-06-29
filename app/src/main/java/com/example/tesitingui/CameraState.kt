package com.example.tesitingui


import android.graphics.Bitmap

data class CameraState(
    val capturedImage: Bitmap? = null,
    var capturingInProgress : Boolean = false,
    var imageText : String = ""
)