package com.mario.totp

import retrofit2.http.GET
import retrofit2.http.Url

interface TotpApi {
    @GET
    suspend fun fetchSecrets(@Url url: String): Map<String, String>
}
