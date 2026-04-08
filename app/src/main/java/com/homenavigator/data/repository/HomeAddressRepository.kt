package com.homenavigator.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.homenavigator.data.model.HomeAddress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("home_prefs")

@Singleton
class HomeAddressRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_NAME = stringPreferencesKey("home_display_name")
    private val KEY_LAT  = doublePreferencesKey("home_latitude")
    private val KEY_LNG  = doublePreferencesKey("home_longitude")
    private val KEY_OK   = booleanPreferencesKey("home_configured")

    val homeAddress: Flow<HomeAddress> = context.dataStore.data.map { p ->
        HomeAddress(p[KEY_NAME] ?: "", p[KEY_LAT] ?: 0.0, p[KEY_LNG] ?: 0.0, p[KEY_OK] ?: false)
    }

    suspend fun saveHomeAddress(a: HomeAddress) {
        context.dataStore.edit { p ->
            p[KEY_NAME] = a.displayName; p[KEY_LAT] = a.latitude
            p[KEY_LNG]  = a.longitude;  p[KEY_OK]  = true
        }
    }

    suspend fun clearHomeAddress() {
        context.dataStore.edit { p ->
            p[KEY_OK] = false; p[KEY_NAME] = ""; p[KEY_LAT] = 0.0; p[KEY_LNG] = 0.0
        }
    }
}