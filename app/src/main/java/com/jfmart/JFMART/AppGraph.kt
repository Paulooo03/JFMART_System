package com.jfmart.JFMART

import android.app.Application
import android.content.Context
import com.jfmart.JFMART.data.remote.ApiService
import com.jfmart.JFMART.data.session.SessionStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** The app's shared services, created once at startup. */
object AppGraph {
    lateinit var session: SessionStore
        private set
    lateinit var api: ApiService
        private set

    fun init(context: Context) {
        session = SessionStore(context.applicationContext)

        val http = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = session.current?.token
                val request = if (token == null) chain.request()
                else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                val response = chain.proceed(request)
                // An expired or rejected token signs the user out; navigation then returns to the login screen
                if (response.code == 401 && token != null) session.clear()
                response
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

class JfMartApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
