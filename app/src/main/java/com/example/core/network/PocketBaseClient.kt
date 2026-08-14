package com.example.core.network

import com.example.core.datastore.SessionDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class PocketBaseClient(
    private val baseUrl: String = PocketBaseConstants.BASE_URL,
    private val sessionDataStore: SessionDataStore? = null
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var cachedToken: String? = null

    fun setAuthToken(token: String?) {
        cachedToken = token
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")

            val token = cachedToken
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", token)
            }
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun getFullUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    fun getFileUrl(collectionName: String, recordId: String, fileName: String, thumb: String? = null): String {
        if (fileName.isBlank()) return ""
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) return fileName
        val url = getFullUrl("api/files/$collectionName/$recordId/$fileName")
        return if (thumb != null) "$url?thumb=$thumb" else url
    }

    private suspend fun <T> executeRequest(
        request: Request,
        responseClass: Class<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                if (responseClass == Unit::class.java || responseClass == Void::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext NetworkResult.Success(Unit as T)
                }
                if (responseClass == Boolean::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext NetworkResult.Success(true as T)
                }
                val adapter = moshi.adapter(responseClass)
                val parsed = adapter.fromJson(bodyString)
                if (parsed != null) {
                    NetworkResult.Success(parsed)
                } else {
                    NetworkResult.Error("Failed to parse response", response.code)
                }
            } else {
                var message = "Request failed with code ${response.code}"
                try {
                    val errorObj = JSONObject(bodyString)
                    if (errorObj.has("message")) {
                        message = errorObj.getString("message")
                    }
                    if (errorObj.has("data")) {
                        val dataObj = errorObj.getJSONObject("data")
                        val firstKey = dataObj.keys().asSequence().firstOrNull()
                        if (firstKey != null) {
                            val detail = dataObj.getJSONObject(firstKey).optString("message")
                            if (detail.isNotBlank()) {
                                message = "$firstKey: $detail"
                            }
                        }
                    }
                } catch (e: Exception) {
                    // fallback to standard message
                }
                NetworkResult.Error(message, response.code, bodyString)
            }
        } catch (e: IOException) {
            NetworkResult.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
        } catch (e: Exception) {
            NetworkResult.Error("Unexpected error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    // AUTH API
    suspend fun authWithPassword(
        identity: String,
        password: String
    ): NetworkResult<PBAuthResponse> {
        val payload = JSONObject().apply {
            put("identity", identity)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.AUTH_WITH_PASSWORD))
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        val result = executeRequest(request, PBAuthResponse::class.java)
        if (result is NetworkResult.Success) {
            setAuthToken(result.data.token)
        }
        return result
    }

    suspend fun register(
        email: String,
        username: String,
        password: String,
        passwordConfirm: String,
        name: String
    ): NetworkResult<PBUserRecord> {
        val payload = JSONObject().apply {
            put("email", email)
            put("username", username)
            put("password", password)
            put("passwordConfirm", passwordConfirm)
            put("name", name)
            put("emailVisibility", true)
        }.toString()

        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.collection(PocketBaseConstants.Collections.USERS)))
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return executeRequest(request, PBUserRecord::class.java)
    }

    suspend fun authRefresh(): NetworkResult<PBAuthResponse> {
        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.AUTH_REFRESH))
            .post("{}".toRequestBody(jsonMediaType))
            .build()

        val result = executeRequest(request, PBAuthResponse::class.java)
        if (result is NetworkResult.Success) {
            setAuthToken(result.data.token)
        }
        return result
    }

    suspend fun requestVerification(email: String): NetworkResult<Boolean> {
        val payload = JSONObject().apply {
            put("email", email)
        }.toString()

        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.REQUEST_VERIFICATION))
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return executeRequest(request, Boolean::class.java)
    }

    suspend fun confirmVerification(token: String): NetworkResult<Boolean> {
        val payload = JSONObject().apply {
            put("token", token)
        }.toString()

        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.CONFIRM_VERIFICATION))
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return executeRequest(request, Boolean::class.java)
    }

    suspend fun requestPasswordReset(email: String): NetworkResult<Boolean> {
        val payload = JSONObject().apply {
            put("email", email)
        }.toString()

        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.REQUEST_PASSWORD_RESET))
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return executeRequest(request, Boolean::class.java)
    }

    suspend fun confirmPasswordReset(
        token: String,
        password: String,
        passwordConfirm: String
    ): NetworkResult<Boolean> {
        val payload = JSONObject().apply {
            put("token", token)
            put("password", password)
            put("passwordConfirm", passwordConfirm)
        }.toString()

        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.CONFIRM_PASSWORD_RESET))
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        return executeRequest(request, Boolean::class.java)
    }

    // GENERIC COLLECTIONS API
    suspend fun <T> getList(
        collectionName: String,
        page: Int = 1,
        perPage: Int = 30,
        filter: String? = null,
        sort: String? = "-created",
        expand: String? = null,
        itemType: Class<T>
    ): NetworkResult<PBListResponse<T>> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = getFullUrl(PocketBaseConstants.Endpoints.collection(collectionName))
                .toHttpUrlOrNull()?.newBuilder() ?: return@withContext NetworkResult.Error("Invalid URL")

            urlBuilder.addQueryParameter("page", page.toString())
            urlBuilder.addQueryParameter("perPage", perPage.toString())
            if (!filter.isNullOrBlank()) urlBuilder.addQueryParameter("filter", filter)
            if (!sort.isNullOrBlank()) urlBuilder.addQueryParameter("sort", sort)
            if (!expand.isNullOrBlank()) urlBuilder.addQueryParameter("expand", expand)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val listType = Types.newParameterizedType(PBListResponse::class.java, itemType)
                val adapter = moshi.adapter<PBListResponse<T>>(listType)
                val parsed = adapter.fromJson(bodyString)
                if (parsed != null) {
                    NetworkResult.Success(parsed)
                } else {
                    NetworkResult.Error("Failed to parse list response", response.code)
                }
            } else {
                NetworkResult.Error("Request failed: ${response.message}", response.code, bodyString)
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error fetching list: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun <T> getRecord(
        collectionName: String,
        id: String,
        expand: String? = null,
        itemType: Class<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = getFullUrl(PocketBaseConstants.Endpoints.record(collectionName, id))
                .toHttpUrlOrNull()?.newBuilder() ?: return@withContext NetworkResult.Error("Invalid URL")

            if (!expand.isNullOrBlank()) urlBuilder.addQueryParameter("expand", expand)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            executeRequest(request, itemType)
        } catch (e: Exception) {
            NetworkResult.Error("Error fetching record: ${e.localizedMessage}")
        }
    }

    suspend fun <T> createRecord(
        collectionName: String,
        bodyMap: Map<String, Any?>,
        itemType: Class<T>
    ): NetworkResult<T> {
        val payload = JSONObject(bodyMap).toString()
        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.collection(collectionName)))
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return executeRequest(request, itemType)
    }

    suspend fun <T> updateRecord(
        collectionName: String,
        id: String,
        bodyMap: Map<String, Any?>,
        itemType: Class<T>
    ): NetworkResult<T> {
        val payload = JSONObject(bodyMap).toString()
        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.record(collectionName, id)))
            .patch(payload.toRequestBody(jsonMediaType))
            .build()
        return executeRequest(request, itemType)
    }

    suspend fun deleteRecord(
        collectionName: String,
        id: String
    ): NetworkResult<Boolean> {
        val request = Request.Builder()
            .url(getFullUrl(PocketBaseConstants.Endpoints.record(collectionName, id)))
            .delete()
            .build()
        return executeRequest(request, Boolean::class.java)
    }

    suspend fun <T> uploadFile(
        collectionName: String,
        recordId: String?,
        fileField: String,
        file: File,
        extraFields: Map<String, String> = emptyMap(),
        itemType: Class<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            for ((key, value) in extraFields) {
                requestBodyBuilder.addFormDataPart(key, value)
            }

            val mediaType = when {
                file.name.endsWith(".mp4", ignoreCase = true) -> "video/mp4".toMediaType()
                file.name.endsWith(".png", ignoreCase = true) -> "image/png".toMediaType()
                file.name.endsWith(".webp", ignoreCase = true) -> "image/webp".toMediaType()
                else -> "image/jpeg".toMediaType()
            }

            requestBodyBuilder.addFormDataPart(
                fileField,
                file.name,
                file.asRequestBody(mediaType)
            )

            val url = if (recordId.isNullOrBlank()) {
                getFullUrl(PocketBaseConstants.Endpoints.collection(collectionName))
            } else {
                getFullUrl(PocketBaseConstants.Endpoints.record(collectionName, recordId))
            }

            val requestBuilder = Request.Builder().url(url)
            if (recordId.isNullOrBlank()) {
                requestBuilder.post(requestBodyBuilder.build())
            } else {
                requestBuilder.patch(requestBodyBuilder.build())
            }

            executeRequest(requestBuilder.build(), itemType)
        } catch (e: Exception) {
            NetworkResult.Error("Failed to upload: ${e.localizedMessage}")
        }
    }

    suspend fun <T> uploadFiles(
        collectionName: String,
        recordId: String?,
        fileField: String,
        files: List<File>,
        extraFields: Map<String, String> = emptyMap(),
        itemType: Class<T>
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            for ((key, value) in extraFields) {
                requestBodyBuilder.addFormDataPart(key, value)
            }

            for (file in files) {
                val mediaType = when {
                    file.name.endsWith(".mp4", ignoreCase = true) -> "video/mp4".toMediaType()
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png".toMediaType()
                    file.name.endsWith(".webp", ignoreCase = true) -> "image/webp".toMediaType()
                    else -> "image/jpeg".toMediaType()
                }
                requestBodyBuilder.addFormDataPart(
                    fileField,
                    file.name,
                    file.asRequestBody(mediaType)
                )
            }

            val url = if (recordId.isNullOrBlank()) {
                getFullUrl(PocketBaseConstants.Endpoints.collection(collectionName))
            } else {
                getFullUrl(PocketBaseConstants.Endpoints.record(collectionName, recordId))
            }

            val requestBuilder = Request.Builder().url(url)
            if (recordId.isNullOrBlank()) {
                requestBuilder.post(requestBodyBuilder.build())
            } else {
                requestBuilder.patch(requestBodyBuilder.build())
            }

            executeRequest(requestBuilder.build(), itemType)
        } catch (e: Exception) {
            NetworkResult.Error("Failed to upload files: ${e.localizedMessage}")
        }
    }
}
