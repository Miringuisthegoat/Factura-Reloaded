package com.benjamin.factura.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "factura_settings")

class PreferencesManager(private val context: Context) {
    private val isOnboardingCompleteKey = booleanPreferencesKey("is_onboarding_complete")
    private val defaultCurrencyKey = stringPreferencesKey("default_currency")
    private val businessNameKey = stringPreferencesKey("business_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val activeBusinessIdKey = stringPreferencesKey("active_business_id")

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isOnboardingCompleteKey] ?: false
    }

    val activeBusinessId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[activeBusinessIdKey] ?: "default_business"
    }

    val defaultCurrency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[defaultCurrencyKey] ?: DEFAULT_CURRENCY_CODE
    }

    val businessName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[businessNameKey] ?: "My Business"
    }

    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[userEmailKey] ?: ""
    }

    suspend fun setActiveBusinessId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[activeBusinessIdKey] = id
        }
    }

    suspend fun setOnboardingComplete(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isOnboardingCompleteKey] = completed
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[defaultCurrencyKey] = currency
        }
    }

    suspend fun setBusinessDetails(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[businessNameKey] = name
            preferences[userEmailKey] = email
        }
    }

    companion object {
        const val DEFAULT_CURRENCY_CODE = "KES"
    }
}
