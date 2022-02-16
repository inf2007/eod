package com.singaporetech.eod

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.boliao.eod.R
import com.boliao.eod.databinding.ActivitySplashBinding
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
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
}
