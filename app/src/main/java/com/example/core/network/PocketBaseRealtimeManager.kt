package com.example.core.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class PBRealtimeEvent(
    val action: String, // "create", "update", "delete"
    val collection: String,
    val recordJson: String,
    val clientId: String = ""
)

class PocketBaseRealtimeManager(
    private val client: PocketBaseClient,
    private val baseUrl: String = PocketBaseConstants.BASE_URL
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var eventSource: EventSource? = null
    private var sseClientId: String? = null
    private val isConnected = AtomicBoolean(false)

    private val subscriptions = ConcurrentHashMap<String, MutableSet<String>>() // topic -> set of subscribers

    private val _eventsFlow = MutableSharedFlow<PBRealtimeEvent>(extraBufferCapacity = 64)
    val eventsFlow: SharedFlow<PBRealtimeEvent> = _eventsFlow.asSharedFlow()

    private val sseClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun start() {
        if (isConnected.get()) return
        connect()
    }

    private fun connect() {
        val sseUrl = if (baseUrl.endsWith("/")) "${baseUrl}api/realtime" else "$baseUrl/api/realtime"
        val request = Request.Builder()
            .url(sseUrl)
            .header("Accept", "text/event-stream")
            .build()

        val factory = EventSources.createFactory(sseClient)
        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                isConnected.set(true)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JSONObject(data)
                    val clientId = json.optString("clientId", "")
                    if (clientId.isNotBlank()) {
                        sseClientId = clientId
                        resubscribeAll()
                        return
                    }

                    val action = json.optString("action", "")
                    val recordObj = json.optJSONObject("record")
                    val collection = recordObj?.optString("@collectionName", "")
                        ?: json.optString("collection", "")

                    val event = PBRealtimeEvent(
                        action = action,
                        collection = collection,
                        recordJson = recordObj?.toString() ?: data,
                        clientId = sseClientId ?: ""
                    )

                    scope.launch {
                        _eventsFlow.emit(event)
                    }
                } catch (e: Exception) {
                    // Ignore parse errors on ping/keep-alive
                }
            }

            override fun onClosed(eventSource: EventSource) {
                isConnected.set(false)
                sseClientId = null
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                isConnected.set(false)
                sseClientId = null
                // Attempt reconnect after brief delay
                scope.launch {
                    kotlinx.coroutines.delay(5000)
                    if (!isConnected.get()) {
                        connect()
                    }
                }
            }
        })
    }

    private fun resubscribeAll() {
        val cid = sseClientId ?: return
        val allSubscriptions = subscriptions.keys().toList()
        if (allSubscriptions.isEmpty()) return

        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("clientId", cid)
                    put("subscriptions", org.json.JSONArray(allSubscriptions))
                }.toString()

                client.okHttpClient.newCall(
                    Request.Builder()
                        .url("${baseUrl.trimEnd('/')}/api/realtime")
                        .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
                        .build()
                ).execute()
            } catch (e: Exception) {
                // Subscription error
            }
        }
    }

    private val _typingFlow = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 32)
    val typingFlow: SharedFlow<Pair<String, Boolean>> = _typingFlow.asSharedFlow()

    fun emitTyping(conversationId: String, isTyping: Boolean) {
        scope.launch {
            _typingFlow.emit(conversationId to isTyping)
        }
    }

    fun emitLocalEvent(event: PBRealtimeEvent) {
        scope.launch {
            _eventsFlow.emit(event)
        }
    }

    fun subscribe(topic: String, subscriberKey: String = "default") {
        val subscribers = subscriptions.computeIfAbsent(topic) { ConcurrentHashMap.newKeySet() }
        subscribers.add(subscriberKey)
        resubscribeAll()
    }

    fun unsubscribe(topic: String, subscriberKey: String = "default") {
        subscriptions[topic]?.remove(subscriberKey)
        if (subscriptions[topic]?.isEmpty() == true) {
            subscriptions.remove(topic)
        }
        resubscribeAll()
    }

    fun eventsForCollection(collectionName: String): kotlinx.coroutines.flow.Flow<PBRealtimeEvent> {
        return eventsFlow.filter { it.collection == collectionName }
    }

    fun stop() {
        eventSource?.cancel()
        eventSource = null
        isConnected.set(false)
        sseClientId = null
    }
}
