package com.example.alldebrid.data

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    val status: String,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String?,
    val message: String?
)

data class UserResponse(
    val user: UserInfo?
)

data class UserInfo(
    val username: String?,
    val email: String?,
    @SerializedName("isPremium") val isPremium: Boolean?,
    @SerializedName("premiumUntil") val premiumUntil: Long?
)

data class MagnetUploadResponse(
    val magnets: List<MagnetUploadResult>?
)

data class MagnetUploadResult(
    val magnet: String?,
    val hash: String?,
    val id: Long?,
    val name: String?,
    val size: Long?,
    val ready: Boolean?,
    val error: ApiError?
)

data class MagnetStatusSingleResponse(
    val magnets: Magnet?
)

data class MagnetStatusListResponse(
    val magnets: List<Magnet>?
)

data class Magnet(
    val id: Long?,
    @SerializedName("filename", alternate = ["name", "display_name"]) val filename: String?,
    val size: Long?,
    val hash: String?,
    val status: String?,
    val statusCode: Int?,
    val downloaded: Long?,
    val uploaded: Long?,
    val seeders: Int?,
    val downloadSpeed: Long?,
    val processingPerc: Double?,
    val uploadDate: Long?,
    val completionDate: Long?,
    @SerializedName("links", alternate = ["files"]) val links: List<MagnetLink>?
)

data class MagnetLink(
    @SerializedName("link", alternate = ["l"]) val link: String?,
    @SerializedName("filename", alternate = ["n"]) val filename: String?,
    @SerializedName("size", alternate = ["s"]) val size: Long?,
    @SerializedName("elements", alternate = ["e"]) val elements: List<MagnetLink>? = null
)

data class LinkUnlockResponse(
    val link: String?,
    val filename: String?,
    val filesize: Long?,
    val host: String?
)

data class UserLinksResponse(
    val links: List<SavedLink>?
)

data class SavedLink(
    val link: String?,
    val filename: String?,
    val size: Long?,
    val date: Long?,
    val host: String?
)

data class MagnetDeleteResponse(
    val message: String?
)

data class LinkDeleteResponse(
    val message: String?
)

data class HostsResponse(
    val hosts: Map<String, HostInfo>?,
    val streams: Map<String, HostInfo>?,
    val redirectors: Map<String, HostInfo>?
)

data class HostInfo(
    val name: String?,
    val status: Boolean?,
    val type: String?,
    val domains: List<String>?,
    val extensions: List<String>?
)
