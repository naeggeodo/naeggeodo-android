package com.naeggeodo.data.api

import com.naeggeodo.domain.model.MyInfo
import com.naeggeodo.domain.model.MyNickName
import retrofit2.Response
import retrofit2.http.*

interface InfoApi {
    @GET("user/{user_id}/mypage")
    suspend fun getMyInfo(
        @Path("user_id") userId: String
    ): Response<MyInfo>

    @GET("user/{user_id}/nickname")
    suspend fun getMyNickName(
        @Path("user_id") userId: String
    ): Response<MyNickName>

    @PATCH("user/{user_id}/nickname")
    suspend fun changeNickName(
        @Path("user_id") userId: String,
        @Query("value") nickname: String
    ): Response<MyNickName>
    @POST("report")
    suspend fun report(
        @Body body: HashMap<String, String>
    ): Response<String>
}