package com.mario.totp

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface TotpApi {
    @GET
    suspend fun fetchSecrets(@Url url: String): Map<String, String>

    @POST
    suspend fun pushSecrets(@Url url: String, @Body secrets: Map<String, String>)
}
