package com.homenavigator.data.repository

import com.homenavigator.data.model.OrsDirectionsRequest
import com.homenavigator.data.model.OrsDirectionsResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface OrsApiService {
    @POST("v2/directions/{profile}")
    suspend fun getDirections(
        @Header("Authorization") apiKey: String,
        @Path("profile") profile: String = "driving-car",
        @Body request: OrsDirectionsRequest
    ): OrsDirectionsResponse
}