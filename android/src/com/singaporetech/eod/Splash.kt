/**
 * # WEEK10: NDK (and more...)
 * Communicating data across kotlin and C/C++ code.
 *
 * 1. Add NDK development capabilities to existing project
 * 2. Viewing some NDK examples
 *    - reusing common C++ code (OpenGL, Vulkan)
 *    - interfacing with a native C lib (ARCore)
 * 3. Working with machine learning models ( without directly touching NDK :) )
 */

package com.singaporetech.eod

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.singaporetech.eod.databinding.ActivitySplashBinding
import com.singaporetech.eod.ml.InceptionV31Metadata1
import com.singaporetech.eod.util.YuvToRgbConverter
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Splash View to show the entry screen of the app.
 * - NOTE that some it may be more precise to refer to this as the Controller and the XML as the view
 * - MVCVM? :/
 * - shows some status info
 * - handles user login to enter the game
 */
class Splash : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var startAndroidLauncher: Intent
    private lateinit var binding:ActivitySplashBinding

    // TODO NDK 0: install required dependencies (from Android Studio SDK Tools)
    // - NDK: Android toolset to communicate with native code
    // - CMake: native build tool
    // - LLDB: native code debugger

    // TODO NDK 1: create the native code and build configuration
    // - add C++ module to the app using the Android Studio interface
    // - check the CMake build script (auto-generated)
    //   - check that the .cpp path (relative to script) is correct in the CMake script
    //   - you can add_library for your own libs and find_library for Android NDP native libs
    //   - link the libs together using target_link_libraries

    // TODO NDK 2: create a test method in native to receive a string and show it
    // - declare a native function you want to write in C/C++
    // - edit the new .cpp file that was auto-generated (use auto-complete to match the names)
    // - load native lib in kotlin
    // - in kotlin, receive a string from the native function and display it in a toast
    // - try and debug within native using <android/log.h>

    // TODO NDK 2: declare a native function to write in C/C++
    private external fun getNativeString(): String

    // TODO ML 1: add some camera vars.
    private lateinit var preview: Preview // Preview use case, fast, responsive view of the camera
    private lateinit var imageAnalyzer: ImageAnalysis // Analysis use case, for running ML code
    private lateinit var camera: Camera
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // Get the VM with the dependencies injected through a Factory pattern
    private val splashViewModel: SplashViewModel by viewModels {
        val app = application as EODApp
        SplashViewModelFactory(app.playerRepo, app.weatherRepo)
    }

    /**
     * Helper function to start the game.
     * Android launcher will start the game state service.
     */
    fun launchGame() {
        startActivity(startAndroidLauncher)
    }

    /**
     * Setup all the UI elements and their connections with the VM.
     * @param savedInstanceState the usual bundle of joy
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init launch game intent
        // NOTE I have more comments than necessary for demo purposes
        startAndroidLauncher = Intent(this@Splash, AndroidLauncher::class.java)

        // show splash text by default
        binding.msgTxtview.setText(R.string.welcome_note)

        // observe the weather data
        splashViewModel.weatherData.observe(this, Observer {
            binding.weatherTxtview.text = it
        })

        // PLAY button actions
        binding.playBtn.setOnClickListener {
            val name = binding.nameEdtxt.text.toString()
            val age =
                    if (binding.ageEdtxt.text.toString() == "") 0
                    else binding.ageEdtxt.text.toString().toInt()

            // call a coroutine in the VM to do the login
            splashViewModel.loginWithCoroutines(name, age)
        }

        // observe login status changes from the VM
        splashViewModel.loginStatus.observe(this) {
            if (it) {
                binding.msgTxtview.text = "logging in..."

                // NOTE that launchGame is launching a View so should be here
                launchGame()
            } else
                binding.msgTxtview.text = "Name OREDI exists lah..."
        }

        // provide a way to stop the service
        binding.exitBtn.setOnClickListener {
            stopService(AndroidLauncher.startServiceIntent)
            finish()
        }

        // TODO ML 3: Init additional view elements.
        // 1. start camera view if permissions are granted
        // 2. initialize recyclerview for showing inference results
        // 3. observe results to display in recycler view
        // start camera view if permissions granted
        if (allPermissionsGranted()) {
            startCamera()
        }
        // else request permissions
        else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // init recyclerview for inference results
        val viewAdapter = InferenceOutputsAdapter(this)
        binding.outputsRecyclerview.adapter = viewAdapter

        // disable recyclerview animation to reduce flickering
        binding.outputsRecyclerview.itemAnimator = null

        // TODO ML 6: do something interesting when detected an object in the camera
        // 1. display top inference output in the view
        // 2. unlock secret backdoor login when a particular object is within sight
        // observe inference outputs to display in recyclerview
        splashViewModel.inferenceOutputList.observe(this) {
            viewAdapter.submitList(it)
            if (it[0].label == "refrigerator") {
                binding.msgTxtview.text = "UNLOCKED SECRET \nbackdoor login..."
                launch {
                    delay(3000)
                    launchGame()
                }
            }
        }

        // TODO NDK 2: show the string from native in a Toast here
        Toast.makeText(this, getNativeString(), Toast.LENGTH_LONG).show()
        binding.msgTxtview.text = getNativeString()
    }

    // TODO ML 4: add boilerplate to handle camera permissions
    /**
     * Check all permissions are granted - use for Camera permission in this example.
     * [code from https://github.com/hoitab/TFLClassify]
     */
    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * This gets called after the Camera permission pop up is shown.
     * [code from https://github.com/hoitab/TFLClassify]
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // Exit the app if permission is not granted
                // Best practice is to explain and offer a chance to re-request but this is out of
                // scope in this sample. More details:
                // https://developer.android.com/training/permissions/usage-notes
                Toast.makeText(
                    this,
                    getString(R.string.permission_deny_text),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Start the Camera which involves:
     *
     * 1. Initialising the preview use case
     * 2. Initialising the image analyser use case
     * 3. Attach both to the lifecycle of this activity
     * 4. Pipe the output of the preview object to the PreviewView on the screen
     *
     * [code adapted from https://github.com/hoitab/TFLClassify]
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                // This sets the ideal size for the image to be analyse, CameraX will choose the
                // the most suitable resolution which may not be exactly the same or hold the same
                // aspect ratio
                .setTargetResolution(Size(224, 224))
                // How the Image Analyser should pipe in input, 1. every frame but drop no frame, or
                // 2. go to the latest frame and may drop some frame. The default is 2.
                // STRATEGY_KEEP_ONLY_LATEST. The following line is optional, kept here for clarity
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase: ImageAnalysis ->
                    analysisUseCase.setAnalyzer(cameraExecutor, ImageAnalyzer(this) { items ->
                        // updating the list of recognised objects
                        splashViewModel.updateInferenceOutputs(items)
                    })
                }

            // Select camera, back is the default. If it is not available, choose front camera
            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera - try to bind everything at once and CameraX will find
                // the best combination.
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                // Attach the preview to preview view, aka View Finder
                preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRestart() {
        super.onRestart()
        binding.msgTxtview.text = ""
    }

    /**
     * Example exercise of using a coroutine to perform the above weather update task
     * (we ignore our nice arch layers first to illustrate how coroutines function)
     * 1. add a coroutine scope to the activity using the MainScope delegate
     * 2. write a suspend function to perform the mock network fetch
     * 3. launch a coroutine block in onResume to run the non-blocking network task
     *
     * NOTE
     *   - .launch is fire and forget, .async is execute for a deferred result
     *   - the launch block below is non-blocking
     */
    /*
    override fun onResume() {
        super.onResume()

         onResumeLaunch()
    }
    */

    fun onResumeLaunch() {
        var result1 = "empty1"
        var result2 = "empty2"

        val startTime = System.currentTimeMillis()

        Log.i(TAG, "1 time taken: ${System.currentTimeMillis()-startTime}")

        // start one coroutine a.ka. starting a thread
        launch {
            result1 = fetchWeather1()
            Log.i(TAG, "2 time taken: ${System.currentTimeMillis()-startTime}")
        }

        // start another coroutine
        launch {
            result2 = fetchWeather2()
            Log.i(TAG, "3 time taken: ${System.currentTimeMillis()-startTime}")
        }

        binding.weatherTxtview.text = "Weather is ${result1} ${result2}"
        Log.i(TAG, "4 time taken: ${System.currentTimeMillis()-startTime}")

        Log.i(TAG, "5 time taken: ${System.currentTimeMillis()-startTime}")

    }

    /**
     * Mock fetch weather funcs:
     * - assume fetchWeather1 and 2 are independent and can be performed together
     * NOTE that for example sake this logic is here but this is of course to be done at lower
     *      layers in the architecture.
     * NOTE also the qualified return syntax so that it returns the value at the withContext scope
     */
    private suspend fun fetchWeather1(): String = withContext(Dispatchers.IO) {
        Log.i(TAG, "fetchWeather1 start... in thread ${Thread.currentThread().name}")
        // mock long running task
        delay(2000)
        Log.i(TAG, "fetchWeather1 end... ")
        return@withContext "SUNNY"
    }
    private suspend fun fetchWeather2(): String = withContext(Dispatchers.IO) {
        Log.i(TAG, "fetchWeather2 start... in thread ${Thread.currentThread().name}")
        // mock long running task
        delay(5000)
        Log.i(TAG, "fetchWeather2 end... ")
        return@withContext "AND HUMID"
    }

    /**
     * The "right" way to do the weather task...
     */
    fun onResumeAsync() {
        super.onResume()

        var result1 = "empty1"
        var result2 = "empty2"

        val startTime = System.currentTimeMillis()

        Log.i(TAG, "1 time taken: ${System.currentTimeMillis()-startTime}")
        // starting a coroutine (a.k.a modern thread) which is non-blocking
        // fire and forget
        launch {
            val result1 = async {fetchWeather1()}
            val result2 = async {fetchWeather2()}
            binding.weatherTxtview.text = "Weather is ${result1.await()} ${result2.await()}"
            Log.i(TAG, "2 time taken: ${System.currentTimeMillis()-startTime}")
        }

        Log.i(TAG, "3 time taken: ${System.currentTimeMillis()-startTime}")

    }

    /**
     * Suspend function to perform mock network task.
     */
    private suspend fun fetchMockWeather(): String = withContext(Dispatchers.Main) {
//        Thread.sleep(5000)
        delay(5000)
        return@withContext "Todays mockery is ${Random.nextInt()}"
    }

    companion object {
        private val TAG = Splash::class.simpleName

        // TODO NDK 2: load the native library to make it available for use here
        init {
            System.loadLibrary("eod")
        }

        // TODO ML 1: Some consts for managing the ML stuff
        private const val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA) // permission needed
        private const val NUM_PROBS_TO_DISPLAY = 1 // Maximum number of results displayed

        /**
         * TODO THREADING 2.1: observe the asynctask approach for fetching (mock) weather updates
         * [DEPRECATED] AsyncTask to "encrypt" username
         * - static class so as to prevent leaks
         * - internal ctor to only allow enclosing class to construct
         * - onProgressUpdate(Integer... progress) left as an exercise
         *   publishProgress(Integer) is in built to pass progress to above from doInBackground
         */
        private class WeatherTask internal constructor(act: Splash) : AsyncTask<String?, Void?, Boolean>() {
            // hold the Activity to get all the UI elements
            // - use weak reference (a.k.a. share_ptr) so that it does not leak mem when
            //   activity gets killed
            var wr_splash: WeakReference<Splash> = WeakReference(act)

            override fun onPreExecute() {
                super.onPreExecute()
                val splash = wr_splash.get()
                splash?.let {
                    it.binding.weatherTxtview.text ="fetching weather"
                }
            }

            /**
             * Heavy lifting in the background to be posted back to UI
             * @param strs is a list of the data type we indicate (another thing to trip the unwary)
             * @return Boolean to indicate whether weather fetching was successful
             */
            override fun doInBackground(vararg strs: String?): Boolean {
                try {
                    // mocking long running task
                    Thread.sleep(3000)

                    // do something to the str
                    strs?.let {
                        Log.i(TAG, "in background of AsyncTask processing weather for ${it[0]}")
                    }
                } catch (e: InterruptedException) {
                    return false
                }
                return true
            }

            /**
             * Stuff to be done of the main thread after done background processing.
             * @param b the value returned from the processing
             */
            override fun onPostExecute(b: Boolean) {
                super.onPostExecute(b)
                val splash = wr_splash.get()
                splash?.let {
                    it.binding.weatherTxtview.text ="Today's weather is sunny"
                }
            }
        }
    }

    /**
     * TODO ML 5: define a class to handle the ML results derived from ImageAnalysis.Analyzer
     * - note that you can load a new tflite model for ML using the Android Studio interface
     * - you can download pre-trained models at https://tfhub.dev
     * - you can also train your own model using datasets at https://www.tensorflow.org/datasets
     *
     * [code adapted from https://github.com/hoitab/TFLClassify]
     */
    private class ImageAnalyzer(ctx: Context, private val listener: RecognitionListener) :
        ImageAnalysis.Analyzer {

        // - load the inception V3.1 model
        private val inceptionModel = InceptionV31Metadata1.newInstance(ctx)

        override fun analyze(imageProxy: ImageProxy) {

            val items = mutableListOf<InferenceOutput>()

            // convert Image to Bitmap then to TensorImage
            val tfImage = TensorImage.fromBitmap(toBitmap(imageProxy))

            // run image through the trained model then sort and take top results
            val outputs = inceptionModel.process(tfImage).probabilityAsCategoryList
                .apply {
                    sortByDescending {
                        it.score
                    }
                }
                .take(NUM_PROBS_TO_DISPLAY)

            // accumulate results in a list
            for (output in outputs) {
                items.add(InferenceOutput(output.label, output.score))
            }

            // return results to listener
            listener(items.toList())

            // close the image which triggers processing of the next image to the analyzer
            imageProxy.close()
        }

        /**
         * Convert Image Proxy to Bitmap.
         */
        private val yuvToRgbConverter = YuvToRgbConverter(ctx)
        private lateinit var bitmapBuffer: Bitmap
        private lateinit var rotationMatrix: Matrix

        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
        private fun toBitmap(imageProxy: ImageProxy): Bitmap? {

            val image = imageProxy.image ?: return null

            // Initialise Buffer
            if (!::bitmapBuffer.isInitialized) {
                // The image rotation and RGB image buffer are initialized only once
                Log.d(TAG, "Initalise toBitmap()")
                rotationMatrix = Matrix()
                rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                bitmapBuffer = Bitmap.createBitmap(
                    imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
                )
            }

            // Pass image to an image analyser
            yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)

            // Create the Bitmap in the correct orientation
            return Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                rotationMatrix,
                false
            )
        }

    }
}

// Listener for the result of the ImageAnalyzer
typealias RecognitionListener = (inferenceOutput: List<InferenceOutput>) -> Unit