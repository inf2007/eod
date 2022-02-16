package com.singaporetech.eod

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException

/**
 * Splash VM to manage the data logic between splash view and the model
 */
class SplashViewModel(
        private val playerRepo: PlayerRepo,
        private val weatherRepo: WeatherRepo)
    : ViewModel() {

    // live login status
    // NOTE that LiveData is a type of lifecycle-aware component
    // - manage functions that react to LifecycleOwners - e.g., Activity/Fragments/Services
    // - rather than the gazillion-responsibility dictatorship Activity class handling components
    //   using the onStart(), onResume() etc, now the responsibility falls on the individuals,
    //   like empowering students to do student-directed learning
    // - we can manually add lifecycle-aware components with
    //   someLifecycleOwner.getLifeCycle().addObserver(SomeLifecycleObserver())
    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> = _loginStatus

    // live member records from the Room DB
    val allPlayers: LiveData<List<Player>> = playerRepo.allPlayers.asLiveData()

    // live weather data (read-only)
    // - this is bound to the mutable one in repo
    var weatherData: LiveData<String>

    init {
        // fetch online weather through the repo
        viewModelScope.launch {
            weatherRepo.fetchOnlineWeatherData()
        }

        // link up live data to repo (observer pattern)
        weatherData = weatherRepo.weatherData
    }


    /**
     * TODO THREADING 3*: coroutine approach for login task.
     * Login using a username
     * Runs a coroutine in the VM in-built scope
     * - note that the viewModelScope is an extension func of ViewModel from lifecycle-viewmodel-ktx
     */
    fun loginWithCoroutines(username:String, age:Int?) = viewModelScope.launch(Dispatchers.IO) {
        if (playerRepo.contains(username))
            _loginStatus.postValue(false)
        else {
            playerRepo.insert(Player(username, age, null))
            _loginStatus.postValue(true)

            // perform the pw generation
            generatePwAndUpdate(username)
        }
    }

    /**
     * Generate a password through a mock long running task.
     * Use Dispatchers.Default to place this work to the background Default thread in case the
     * caller of this coroutine is calling via Dispatchers.Main .
     *
     * NOTE that we use withContext to make this function independently main-safe so that it does
     *      not matter what coroutine dispatcher context the caller is in
     * NOTE that this will likely still continue to finish even if the app is placed in the
     *      background. This makes intent services obsolete.
     *
     * @param username the username string
     * @return String password
     */
    private suspend fun generatePwAndUpdate(username: String) = withContext(Dispatchers.IO) {
        Thread.sleep(5000)
        val pw = username + "888888"
        playerRepo.updatePw(username, pw)

        delay(6000) // coroutine method
        val players = playerRepo.getPlayer(username)
        Log.d(TAG, "in generatePwAndUpdate just added pw = ${players[0].pw}")
    }

    override fun onCleared() {
        super.onCleared()

        // by right there is no need to perform this as viewModelScope manages everything auto
        // viewModelScope.cancel()
    }

    companion object {
        private val TAG = SplashViewModel::class.simpleName
    }
}

/**
 * A factory to create the ViewModel properly.
 * Very boilerplatey code...
 * NOTE This is due to the fact that we have ctor params.
 *   ViewModelProviders manage the lifecycle of VMs and we cannot create VMs by ourselves.
 *   So we need to provide a Factory to ViewModelProviders so that it knows how to create for us
 *   whenever we need an instance of it.
 */
class SplashViewModelFactory(
        private val playerRepo: PlayerRepo,
        private val weatherRepo: WeatherRepo)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            return SplashViewModel(playerRepo, weatherRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
