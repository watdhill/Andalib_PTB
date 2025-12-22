package com.example.andalib.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Inisialisasi DataStore di tingkat aplikasi
private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    private val dataStore = context.dataStore
    private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    private val ADMIN_ID_KEY = intPreferencesKey("admin_id")
    private val ADMIN_NAME_KEY = stringPreferencesKey("admin_name")

    val authToken: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }

    val adminId: Flow<Int?> = dataStore.data
        .map { preferences ->
            preferences[ADMIN_ID_KEY]
        }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun saveAdminInfo(adminId: Int, adminName: String) {
        dataStore.edit { preferences ->
            preferences[ADMIN_ID_KEY] = adminId
            preferences[ADMIN_NAME_KEY] = adminName
        }
    }

    suspend fun deleteAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(ADMIN_ID_KEY)
            preferences.remove(ADMIN_NAME_KEY)
        }
    }

    // Function untuk get token (digunakan oleh notification service)
    suspend fun getToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }.first()
    }

    // Function untuk get admin ID
    suspend fun getAdminId(): Int? {
        return dataStore.data.map { preferences ->
            preferences[ADMIN_ID_KEY]
        }.first()
    }

    // Function untuk get admin name
    suspend fun getAdminName(): String? {
        return dataStore.data.map { preferences ->
            preferences[ADMIN_NAME_KEY]
        }.first()
    }
}