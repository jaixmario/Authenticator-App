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

    fun fetchFromUrl(url: String) {
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://placeholder.com/") // Base URL is required but overwritten by @Url
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(TotpApi::class.java)
                val response = api.fetchSecrets(url)
                val newEntries = response.map { TotpEntry(it.key, it.value) }
                _entries.value = _entries.value + newEntries
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
