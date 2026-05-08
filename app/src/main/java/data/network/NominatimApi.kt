package com.giri.geoalert.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

import com.google.gson.JsonElement

data class GeoJson(
    val type: String,
    val coordinates: JsonElement? = null
)

data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String,
    val geojson: GeoJson? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("polygon_geojson") polygonGeoJson: Int = 1
    ): List<NominatimResult>
}

object NominatimClient {
    val api: NominatimApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "GeoAlertApp/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}