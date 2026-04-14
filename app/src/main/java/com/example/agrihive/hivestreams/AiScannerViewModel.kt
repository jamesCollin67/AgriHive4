package com.example.agrihive.hivestreams

import android.app.Application
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class AiScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadingMessage = MutableLiveData<String>()
    val loadingMessage: LiveData<String> = _loadingMessage

    /** 0 = left, 1 = middle, 2 = right — multi-step analyze UI */
    private val _analysisStep = MutableLiveData(1)
    val analysisStep: LiveData<Int> = _analysisStep

    private val _scanResult = MutableLiveData<Pair<String, Int>?>()
    val scanResult: LiveData<Pair<String, Int>?> = _scanResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Full probability map: label -> percentage (0-100)
    private val _allProbabilities = MutableLiveData<List<Pair<String, Int>>>()
    val allProbabilities: LiveData<List<Pair<String, Int>>> = _allProbabilities

    private var _currentImageUri: Uri? = null
    val currentImageUri: Uri? get() = _currentImageUri

    private var interpreter: Interpreter? = null
    private var labels = listOf<String>()
    private var isModelReady = false

    init {
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelBuffer = loadModelFile("model_unquant.tflite")
                val options = Interpreter.Options()
                options.setNumThreads(4)
                interpreter = Interpreter(modelBuffer, options)
                labels = loadLabels("labels.txt")
                isModelReady = true
                Log.d("AiScannerViewModel", "Interpreter and labels loaded successfully")
            } catch (e: Exception) {
                Log.e("AiScannerViewModel", "Interpreter load failed", e)
                _errorMessage.postValue("AI Engine failed to start: ${e.message}")
            }
        }
    }

    fun analyzeImage(uri: Uri) {
        _currentImageUri = uri
        _isLoading.value = true
        _loadingMessage.value = "Preprocessing image tensor..."
        _analysisStep.value = 1

        viewModelScope.launch {
            try {
                var waitCount = 0
                while (!isModelReady && waitCount < 10) {
                    delay(500)
                    waitCount++
                }

                if (!isModelReady || interpreter == null) {
                    _errorMessage.postValue("AI Engine is still initializing. Please try again.")
                    _isLoading.postValue(false)
                    return@launch
                }

                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                }

                if (bitmap == null) {
                    _errorMessage.postValue("Failed to read image")
                    _isLoading.postValue(false)
                    return@launch
                }

                _loadingMessage.postValue("Running neural network inference...")
                _analysisStep.postValue(2)

                val result = withContext(Dispatchers.Default) {
                    runInference(bitmap)
                }

                _scanResult.postValue(result)
            } catch (e: Exception) {
                Log.e("AiScannerViewModel", "Inference failed", e)
                _errorMessage.postValue("Analysis failed: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun runInference(bitmap: Bitmap): Pair<String, Int> {
        val tflite = interpreter ?: throw IllegalStateException("Interpreter not ready")

        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        val height = inputShape[1]
        val width = inputShape[2]

        val inputBuffer = preprocessBitmap(bitmap, width, height)

        val outputIndex = 0
        val outputShape = tflite.getOutputTensor(outputIndex).shape()
        val numLabels = outputShape[1]
        val probabilityBuffer = ByteBuffer.allocateDirect(numLabels * 4).order(ByteOrder.nativeOrder())

        tflite.run(inputBuffer, probabilityBuffer)
        probabilityBuffer.rewind()

        val probabilities = FloatArray(numLabels)
        probabilityBuffer.asFloatBuffer().get(probabilities)

        var maxIdx = 0
        var maxProb = 0f
        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        val label = if (labels.isNotEmpty() && maxIdx < labels.size) {
            labels[maxIdx]
        } else {
            "Unknown Outcome"
        }

        // Build sorted probability list for all labels (top 5 max)
        val allProbs = probabilities.indices
            .map { i -> Pair(if (labels.isNotEmpty() && i < labels.size) labels[i] else "Label $i", (probabilities[i] * 100).toInt()) }
            .sortedByDescending { it.second }
            .take(5)
        _allProbabilities.postValue(allProbs)

        return Pair(label, (maxProb * 100).toInt())
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val assetFileDescriptor: AssetFileDescriptor = getApplication<Application>().assets.openFd(assetName)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    private fun loadLabels(assetName: String): List<String> {
        val labels = mutableListOf<String>()
        getApplication<Application>().assets.open(assetName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        }
        return labels
    }

    private fun preprocessBitmap(bitmap: Bitmap, width: Int, height: Int): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val inputBuffer = ByteBuffer.allocateDirect(1 * width * height * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixelValue in pixels) {
            val r = ((pixelValue shr 16) and 0xFF) / 255f
            val g = ((pixelValue shr 8) and 0xFF) / 255f
            val b = (pixelValue and 0xFF) / 255f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        inputBuffer.rewind()
        return inputBuffer
    }

    fun doneScan() {
        _scanResult.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        interpreter?.close()
    }
}
