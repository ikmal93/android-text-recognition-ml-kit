package com.ikmal.androidmlkitapp.analyzer

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.ikmal.androidmlkitapp.util.ImageUtils
import java.lang.Exception

class TextAnalyzer(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val result: MutableLiveData<String>,
    private val imageCropPercentages: MutableLiveData<Pair<Int, Int>>
) : ImageAnalysis.Analyzer {

    // TODO: Instantiate TextRecognition detector
    private val detector = TextRecognition.getClient()

    init {
        lifecycle.addObserver(detector)
    }

    // TODO: Add lifecycle observer to properly close ML Kit detectors

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        Log.d("JAMAL", "rotationDegrees : $rotationDegrees")

        // We requested a setTargetAspectRatio, but it's not guaranteed that's what the camera
        // stack is able to support, so we calculate the actual ratio from the first frame to
        // know how to appropriately crop the image we want to analyze.
        val imageHeight = mediaImage.height
        val imageWidth = mediaImage.width

        Log.d("JAMAL", "imageHeight : $imageWidth")
        Log.d("JAMAL", "imageWidth : $imageHeight")
        Log.d("JAMAL", "bagi : ${imageWidth / imageHeight}")

        val actualAspectRatio = imageWidth / imageHeight

        Log.d("JAMAL", "actualAspectRatio : $actualAspectRatio")

        val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
        val cropRect = Rect(0, 0, imageWidth, imageHeight)

        // If the image has a way wider aspect ratio than expected, crop less of the height so we
        // don't end up cropping too much of the image. If the image has a way taller aspect ratio
        // than expected, we don't have to make any changes to our cropping so we don't handle it
        // here.
        val currentCropPercentages = imageCropPercentages.value ?: return
        Log.d("JAMAL", "currentCropPercentages : $currentCropPercentages")
        if (actualAspectRatio > 3) {
            val originalHeightCropPercentage = currentCropPercentages.first
            val originalWidthCropPercentage = currentCropPercentages.second
            Log.d("JAMAL", "originalHeightCropPercentage : $originalHeightCropPercentage")
            Log.d("JAMAL", "originalWidthCropPercentage : $originalWidthCropPercentage")
            imageCropPercentages.value =
                Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
        }

        // If the image is rotated by 90 (or 270) degrees, swap height and width when calculating
        // the crop.
        val cropPercentages = imageCropPercentages.value ?: return
        Log.d("JAMAL", "cropPercentages : $cropPercentages")
        val heightCropPercent = cropPercentages.first
        val widthCropPercent = cropPercentages.second
        Log.d("JAMAL", "heightCropPercent : $heightCropPercent")
        Log.d("JAMAL", "widthCropPercent : $widthCropPercent")
        val (widthCrop, heightCrop) = when (rotationDegrees) {
            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
        }

        Log.d("JAMAL", "widthCrop : $widthCrop")
        Log.d("JAMAL", "heightCrop : $heightCrop")

        cropRect.inset(
            (imageWidth * widthCrop / 2).toInt(),
            (imageHeight * heightCrop / 2).toInt()
        )

        Log.d("JAMAL", "width -> $imageWidth x $widthCrop : 2 = ${(imageWidth * widthCrop / 2).toInt()}")
        Log.d("JAMAL", "height -> $imageHeight x $heightCrop : 2 = ${(imageHeight * heightCrop / 2).toInt()}")


        Log.d("JAMAL", "==================================================================================")

        val croppedBitmap =
            ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect)

        // TODO call recognizeText() once implemented
        recognizeText(InputImage.fromBitmap(croppedBitmap, 0)).addOnCompleteListener {
            imageProxy.close()
        }
    }

//    fun recognizeText() {
//        // TODO Use ML Kit's TextRecognition to analyze frames from the camera live feed.
//    }

    private fun recognizeText(
        image: InputImage
    ): Task<Text> {
        // Pass image to an ML Kit Vision API
        return detector.process(image)
            .addOnSuccessListener { text ->
                // Task completed successfully
                result.value = text.text
            }
            .addOnFailureListener { exception ->
                // Task failed with an exception
                Log.e(TAG, "Text recognition error", exception)
                val message = getErrorMessage(exception)
                message?.let {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
            "Waiting for text recognition model to be downloaded"
        } else exception.message
    }

    companion object {
        private const val TAG = "TextAnalyzer"
    }
}