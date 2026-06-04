package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.social.ChatFocusTracker
import com.example.funlife.social.ChatInteractor
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.SocialFailure
import com.example.funlife.social.model.ChatUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ChatToastTone { Success, Error, Info }

data class ChatToast(val message: String, val tone: ChatToastTone = ChatToastTone.Info)

class FriendChatViewModel(
    application: Application,
    private val currentUserId: Long,
    private val peerPbId: String,
) : AndroidViewModel(application) {

    private val interactor = ChatInteractor(application.applicationContext, currentUserId)

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _toast = MutableStateFlow<ChatToast?>(null)
    val toast: StateFlow<ChatToast?> = _toast.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(false)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    private val _pbAuthToken = MutableStateFlow<String?>(null)
    val pbAuthToken: StateFlow<String?> = _pbAuthToken.asStateFlow()

    private var observeJob: Job? = null
    private var conversationId: String? = null
    private var myPbId: String? = null
    private var historyPage = 1

    init {
        if (!PocketBaseConfig.isEnabled()) {
            _uiState.value = ChatUiState.Error(SocialFailure.NotConfigured.userMessage)
        } else {
            viewModelScope.launch { bootstrap() }
        }
    }

    private suspend fun bootstrap() {
        // 本地缓存优先：经 Cloudflare 隧道时避免每次进聊天都长时间转圈
        interactor.loadCachedSession(peerPbId)?.let { cached ->
            applySession(cached)
            syncMessagesInBackground(cached.conversationId)
            return
        }

        _uiState.value = ChatUiState.Loading
        interactor.bootstrap(peerPbId)
            .onSuccess { boot ->
                applySession(boot)
                syncMessagesInBackground(boot.conversationId)
            }
            .onFailure { e ->
                _uiState.value = ChatUiState.Error(
                    SocialFailure.fromThrowable(e).userMessage,
                )
            }
    }

    private fun applySession(boot: ChatInteractor.ChatBootstrap) {
        conversationId = boot.conversationId
        myPbId = boot.myPbId
        historyPage = 1
        _hasMoreHistory.value = boot.hasMoreHistory
        _pbAuthToken.value = boot.token
        ChatFocusTracker.setFocus(currentUserId, peerPbId)
        startObserving(boot.conversationId, boot.myPbId, boot.peer)
    }

    private fun syncMessagesInBackground(convId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            interactor.syncMessages(convId)
                .onSuccess { hasMore -> _hasMoreHistory.value = hasMore }
                .onFailure { showError(it) }
        }
    }

    private fun startObserving(
        convId: String,
        myId: String,
        peer: com.example.funlife.social.model.ChatPeerProfile,
    ) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            interactor.observeMessages(myId, convId).collect { messages ->
                _uiState.value = ChatUiState.Ready(
                    conversationId = convId,
                    peer = peer,
                    messages = messages,
                )
            }
        }
    }

    fun refresh() {
        val convId = conversationId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            interactor.syncMessages(convId)
                .onSuccess { hasMore -> _hasMoreHistory.value = hasMore }
                .onFailure { showError(it) }
        }
    }

    fun loadOlderMessages() {
        val convId = conversationId ?: return
        if (!_hasMoreHistory.value || _isLoadingHistory.value) return
        val nextPage = historyPage + 1
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHistory.value = true
            try {
                interactor.loadOlderMessages(convId, nextPage)
                    .onSuccess { hasMore ->
                        historyPage = nextPage
                        _hasMoreHistory.value = hasMore
                    }
                    .onFailure { showError(it) }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    fun sendMessage(raw: String) {
        val convId = conversationId ?: return
        if (_isSending.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSending.value = true
            try {
                interactor.sendMessage(convId, raw)
                    .onFailure { showError(it) }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isSending.value = false
            }
        }
    }

    fun retryBootstrap() {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun showError(t: Throwable) {
        withContext(Dispatchers.Main) {
            _toast.value = ChatToast(
                SocialFailure.fromThrowable(t).userMessage,
                ChatToastTone.Error,
            )
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    override fun onCleared() {
        ChatFocusTracker.clearFocus(currentUserId)
        super.onCleared()
    }
}
