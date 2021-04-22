package com.ikmal.androidmlkitapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.ikmal.androidmlkitapp.fragment.MainFragment.Companion.DESIRED_HEIGHT_CROP_PERCENT
import com.ikmal.androidmlkitapp.fragment.MainFragment.Companion.DESIRED_WIDTH_CROP_PERCENT
import com.ikmal.androidmlkitapp.util.Language
import com.ikmal.androidmlkitapp.util.ResultOrError
import com.ikmal.androidmlkitapp.util.SmoothedMutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // TODO Instantiate LanguageIdentification
    val targetLang = MutableLiveData<Language>()
    val sourceText = SmoothedMutableLiveData<String>(SMOOTHING_DURATION)

    // We set desired crop percentages to avoid having to analyze the whole image from the live
    // camera feed. However, we are not guaranteed what aspect ratio we will get from the camera, so
    // we use the first frame we get back from the camera to update these crop percentages based on
    // the actual aspect ratio of images.
    val imageCropPercentages = MutableLiveData<Pair<Int, Int>>()
        .apply { value = Pair(DESIRED_HEIGHT_CROP_PERCENT, DESIRED_WIDTH_CROP_PERCENT) }
    val translatedText = MediatorLiveData<ResultOrError>()
    private val translating = MutableLiveData<Boolean>()
    val modelDownloading = SmoothedMutableLiveData<Boolean>(SMOOTHING_DURATION)

    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    val sourceLang = Transformations.switchMap(sourceText) { text ->
        val result = MutableLiveData<Language>()
        // TODO  Call the language identification method and assigns the result if it is not
        //  undefined (“und”)
        result
    }

    override fun onCleared() {
        // TODO Shut down ML Kit clients.
    }

    private fun translate(): Task<String> {
        // TODO Take the source language value, target language value, and the source text and
        //  perform the translation.
        //  If the chosen target language model has not been downloaded to the device yet,
        //  call downloadModelIfNeeded() and then proceed with the translation.
        return Tasks.forResult("") // replace this with your code
    }


    init {
        modelDownloading.setValue(false)
        translating.value = false
        // Create a translation result or error object.
        val processTranslation =
            OnCompleteListener<String> { task ->
                if (task.isSuccessful) {
                    translatedText.value = ResultOrError(task.result, null)
                } else {
                    if (task.isCanceled) {
                        // Tasks are cancelled for reasons such as gating; ignore.
                        return@OnCompleteListener
                    }
                    translatedText.value = ResultOrError(null, task.exception)
                }
            }
        // Start translation if any of the following change: detected text, source lang, target lang.
        translatedText.addSource(sourceText) { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(sourceLang) { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(targetLang) { translate().addOnCompleteListener(processTranslation) }
    }

    companion object {
        // Amount of time (in milliseconds) to wait for detected text to settle
        private const val SMOOTHING_DURATION = 50L

        private const val NUM_TRANSLATORS = 1
    }
}