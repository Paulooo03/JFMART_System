package com.jfmart.JFMART.data.remote

import android.util.Log
import com.google.gson.Gson
import com.jfmart.JFMART.BuildConfig
import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val code: Int? = null) : ApiResult<Nothing>
}

/** Runs an API call and turns network and server failures into a message the UI can show. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: HttpException) {
    ApiResult.Err(errorMessage(e), e.code())
} catch (e: IOException) {
    // The exact cause (refused, timed out, cleartext blocked...) is in Logcat under the tag "JFMartApi"
    Log.w("JFMartApi", "Network error calling ${BuildConfig.API_BASE_URL}", e)
    ApiResult.Err("Can't reach the server. Check your connection and the server address.")
} catch (e: JsonParseException) {
    ApiResult.Err("The server sent a response the app could not read.")
}

private fun errorMessage(e: HttpException): String {
    val fromBody = try {
        Gson().fromJson(e.response()?.errorBody()?.string(), ErrorBody::class.java)?.message
    } catch (ignored: Exception) {
        null
    }
    return when {
        !fromBody.isNullOrBlank() -> fromBody
        e.code() == 401 -> "Please sign in again."
        e.code() == 403 -> "You are not allowed to do that."
        else -> "Something went wrong (error ${e.code()})."
    }
}
