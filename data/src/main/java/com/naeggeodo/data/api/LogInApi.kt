package com.naeggeodo.data.api

import com.naeggeodo.domain.model.LogIn
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface LogInApi {
    @POST("login/mobil/{provider}")
    suspend fun logIn(
        @Path("provider") provider: String,
        @Body params: HashMap<String, String?>
    ): Response<LogIn>
}