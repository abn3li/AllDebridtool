package com.example.alldebrid.data

import android.util.Log
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    private const val BASE_URL = "https://api.alldebrid.com/"
    const val AGENT = "AllDebridComposeClient"

    val api: AllDebridApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AllDebridApi::class.java)
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

class AllDebridRepository(private val apiKey: String) {
    private val api = RetrofitProvider.api
    private val agent = RetrofitProvider.AGENT

    suspend fun verifyUser(): ApiResult<UserInfo> {
        return try {
            val response = api.getUser(agent, apiKey)
            val user = response.data?.user
            if ((response.status == "success") && (user != null)) {
                ApiResult.Success(user)
            } else {
                ApiResult.Failure(response.error?.message ?: "Invalid API key")
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun uploadMagnet(magnetLink: String): ApiResult<MagnetUploadResult> {
        return try {
            val response = api.uploadMagnet(agent, apiKey, magnetLink)
            val result = response.data?.magnets?.firstOrNull()
            if (response.status == "success" && result != null && result.error == null) {
                ApiResult.Success(result)
            } else {
                ApiResult.Failure(
                    result?.error?.message ?: response.error?.message ?: "Could not add magnet",
                )
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun uploadTorrentFile(filePart: MultipartBody.Part): ApiResult<MagnetUploadResult> {
        return try {
            val response = api.uploadTorrentFile(agent, apiKey, filePart)
            val result = response.data?.magnets?.firstOrNull()
            if (response.status == "success" && result != null && result.error == null) {
                ApiResult.Success(result)
            } else {
                ApiResult.Failure(
                    result?.error?.message ?: response.error?.message ?: "Could not upload torrent file",
                )
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getAllMagnets(): ApiResult<List<Magnet>> {
        return try {
            val response = api.getAllMagnetStatus(agent, apiKey)
            if (response.status == "success") {
                ApiResult.Success(response.data?.magnets ?: emptyList())
            } else {
                ApiResult.Failure(response.error?.message ?: "Could not fetch magnets")
            }
        } catch (e: Exception) {
            Log.e("AllDebridRepo", "getAllMagnets failed to parse response", e)
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getMagnetDetails(id: Long): ApiResult<Magnet> {
        return try {
            val response = api.getMagnetFiles(agent, apiKey, id)
            val magnet = response.data?.magnets?.firstOrNull()
            if (response.status == "success" && magnet != null) {
                ApiResult.Success(magnet)
            } else {
                ApiResult.Failure(response.error?.message ?: "Could not fetch magnet details")
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun deleteMagnet(id: Long): ApiResult<Unit> {
        return try {
            val response = api.deleteMagnet(agent, apiKey, id)
            if (response.status == "success") ApiResult.Success(Unit)
            else ApiResult.Failure(response.error?.message ?: "Could not delete magnet")
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getUserLinks(): ApiResult<List<SavedLink>> {
        return try {
            val response = api.getUserLinks(agent, apiKey)
            if (response.status == "success") {
                ApiResult.Success(response.data?.links ?: emptyList())
            } else {
                ApiResult.Failure(response.error?.message ?: "Could not fetch library")
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getUserHistory(): ApiResult<List<SavedLink>> {
        return try {
            val response = api.getUserHistory(agent, apiKey)
            if (response.status == "success") {
                ApiResult.Success(response.data?.links ?: emptyList())
            } else {
                ApiResult.Failure(response.error?.message ?: "Could not fetch history")
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun deleteSavedLink(link: String): ApiResult<Unit> {
        return try {
            val response = api.deleteUserLinks(agent, apiKey, listOf(link))
            if (response.status == "success") ApiResult.Success(Unit)
            else ApiResult.Failure(response.error?.message ?: "Could not delete link")
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun purgeHistory(): ApiResult<Unit> {
        return try {
            val response = api.purgeHistory(agent, apiKey)
            if (response.status == "success") ApiResult.Success(Unit)
            else ApiResult.Failure(response.error?.message ?: "Could not clear history")
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun unlockLink(link: String): ApiResult<LinkUnlockResponse> {
        return try {
            val response = api.unlockLink(agent, apiKey, link)
            if (response.status == "success" && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Failure(response.error?.message ?: "Could not unlock link")
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getHosts(): ApiResult<HostsResponse> {
        return try {
            val response = api.getHosts(agent, apiKey)
            Log.d("AllDebridRepo", "Hosts raw: $response")
            if (response.status == "success" && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Failure(response.error?.message ?: "Could not fetch hosts")
            }
        } catch (e: Exception) {
            ApiResult.Failure(e.localizedMessage ?: "Network error")
        }
    }
}
