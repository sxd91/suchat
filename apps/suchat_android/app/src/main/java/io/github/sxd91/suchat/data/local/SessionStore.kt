package io.github.sxd91.suchat.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.suchatSessionDataStore by preferencesDataStore(name = "suchat_session")

data class StoredSession(val endpoint: String? = null, val accessToken: String? = null, val previewMode: Boolean = false)

class SessionStore(private val context: Context) {
    private val endpointKey = stringPreferencesKey("endpoint")
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val previewModeKey = booleanPreferencesKey("preview_mode")
    val session: Flow<StoredSession> = context.suchatSessionDataStore.data.map { values -> StoredSession(values[endpointKey], values[accessTokenKey], values[previewModeKey] ?: false) }
    suspend fun saveRemote(endpoint: String, accessToken: String) { context.suchatSessionDataStore.edit { values -> values[endpointKey] = endpoint; values[accessTokenKey] = accessToken; values[previewModeKey] = false } }
    suspend fun enterPreview() { context.suchatSessionDataStore.edit { values -> values.remove(endpointKey); values.remove(accessTokenKey); values[previewModeKey] = true } }
    suspend fun clear() { context.suchatSessionDataStore.edit { values -> values.clear() } }
}
