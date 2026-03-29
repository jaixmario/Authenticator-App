package com.mario.totp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    private val PREFS_NAME = "totp_prefs"
    private val KEY_ENTRIES = "entries"
    private val KEY_API_URL = "api_url"

    fun initData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ENTRIES, null)
        if (json != null) {
            val type = object : TypeToken<List<TotpEntry>>() {}.type
            _entries.value = Gson().fromJson(json, type)
        }

        viewModelScope.launch {
            while (true) {
                _currentTime.value = TotpGenerator.getRemainingSeconds()
                delay(1000)
            }
        }
    }

    private fun saveLocalData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(_entries.value)
        prefs.edit().putString(KEY_ENTRIES, json).apply()
    }

    fun getSavedApiUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API_URL, "") ?: ""
    }

    fun saveApiUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API_URL, url).apply()
    }

    fun addEntry(context: Context, name: String, secret: String) {
        _entries.value = (_entries.value + TotpEntry(name, secret)).distinctBy { it.name }
        saveLocalData(context)
    }

    private fun getRetrofit(): TotpApi {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TotpApi::class.java)
    }

    fun syncWithUrl(context: Context, url: String) {
        saveApiUrl(context, url)
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                val api = getRetrofit()
                
                // 1. Fetch from server
                val fetchedMap = api.fetchSecrets(url)
                val fetchedEntries = fetchedMap.map { TotpEntry(it.key, it.value) }
                
                val currentLocal = _entries.value
                val newFromCloud = fetchedEntries.filter { cloud -> currentLocal.none { it.name == cloud.name } }
                
                // 2. Combine
                val combined = (currentLocal + fetchedEntries).distinctBy { it.name }
                val uploadedCount = combined.size - fetchedEntries.size
                
                _entries.value = combined
                saveLocalData(context)
                
                // 3. Push back
                val mapToPush = combined.associate { it.name to it.secret }
                api.pushSecrets(url, mapToPush)
                
                _syncStatus.value = "Added ${newFromCloud.size}, Uploaded $uploadedCount"
                delay(4000)
                _syncStatus.value = null
            } catch (e: Exception) {
                _syncStatus.value = "Sync Failed: ${e.localizedMessage}"
                delay(3000)
                _syncStatus.value = null
            }
        }
    }
}
