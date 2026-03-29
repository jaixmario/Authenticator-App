package com.mario.totp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {
    private val _entries = MutableStateFlow<List<TotpEntry>>(emptyList())
    val entries: StateFlow<List<TotpEntry>> = _entries

    private val _currentTime = MutableStateFlow(TotpGenerator.getRemainingSeconds())
    val currentTime: StateFlow<Int> = _currentTime

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus

    init {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = TotpGenerator.getRemainingSeconds()
                delay(1000)
            }
        }
    }

    fun addEntry(name: String, secret: String) {
        _entries.value = _entries.value + TotpEntry(name, secret)
    }

    private fun getRetrofit(): TotpApi {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TotpApi::class.java)
    }

    fun syncWithUrl(url: String) {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                val api = getRetrofit()
                
                // 1. Fetch from server
                val fetchedMap = api.fetchSecrets(url)
                val fetchedEntries = fetchedMap.map { TotpEntry(it.key, it.value) }
                
                // 2. Combine with local (avoid duplicates)
                val currentLocal = _entries.value
                val combined = (currentLocal + fetchedEntries).distinctBy { it.name }
                _entries.value = combined
                
                // 3. Push combined back to server
                val mapToPush = combined.associate { it.name to it.secret }
                api.pushSecrets(url, mapToPush)
                
                _syncStatus.value = "Sync Successful!"
                delay(2000)
                _syncStatus.value = null
            } catch (e: Exception) {
                _syncStatus.value = "Sync Failed: ${e.localizedMessage}"
                delay(3000)
                _syncStatus.value = null
            }
        }
    }

    fun fetchFromUrl(url: String) {
        viewModelScope.launch {
            try {
                val api = getRetrofit()
                val response = api.fetchSecrets(url)
                val newEntries = response.map { TotpEntry(it.key, it.value) }
                _entries.value = (_entries.value + newEntries).distinctBy { it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
