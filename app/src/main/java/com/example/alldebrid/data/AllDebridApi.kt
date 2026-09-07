package com.example.alldebrid.data

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AllDebridApi {

    @GET("v4/user")
    suspend fun getUser(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<UserResponse>

    @POST("v4/magnet/upload")
    @FormUrlEncoded
    suspend fun uploadMagnet(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
        @Field("magnets[]") magnet: String,
    ): ApiEnvelope<MagnetUploadResponse>

    @GET("v4.1/magnet/status")
    suspend fun getAllMagnetStatus(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<MagnetStatusListResponse>

    @GET("v4/magnet/files")
    suspend fun getMagnetFiles(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
        @Query("id[]") id: Long,
    ): ApiEnvelope<MagnetStatusListResponse>

    @POST("v4/magnet/delete")
    @FormUrlEncoded
    suspend fun deleteMagnet(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
        @Field("id") id: Long,
    ): ApiEnvelope<MagnetDeleteResponse>

    @GET("v4/user/links")
    suspend fun getUserLinks(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<UserLinksResponse>

    @GET("v4/user/history")
    suspend fun getUserHistory(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<UserLinksResponse>

    @POST("v4/user/links/delete")
    @FormUrlEncoded
    suspend fun deleteUserLinks(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
        @Field("links[]") links: List<String>,
    ): ApiEnvelope<LinkDeleteResponse>

    @POST("v4/user/history/delete")
    suspend fun purgeHistory(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<LinkDeleteResponse>

    @GET("v4/link/unlock")
    suspend fun unlockLink(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
        @Query("link") link: String,
    ): ApiEnvelope<LinkUnlockResponse>

    @GET("v4/hosts")
    suspend fun getHosts(
        @Query("agent") agent: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<HostsResponse>
}
