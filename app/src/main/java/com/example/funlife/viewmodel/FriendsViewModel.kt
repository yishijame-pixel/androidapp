package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.social.FriendsInteractor
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.SocialFailure
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.social.model.FriendshipStatus
import com.example.funlife.social.model.FriendsUiState
import com.example.funlife.social.model.PbUserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FriendsToastTone { Success, Error, Info }

data class FriendsToast(
    val message: String,
    val tone: FriendsToastTone = FriendsToastTone.Info,
)

enum class PendingFriendActionKind { ACCEPT, REJECT }

data class PendingFriendAction(
    val friendshipId: String,
    val kind: PendingFriendActionKind,
)

/**
 * 好友页 ViewModel：仅负责 UI 状态与用户事件，所有社交网络逻辑委托 [FriendsInteractor]。
 */
class FriendsViewModel(
    application: Application,
    currentUserId: Long,
    funlifeUsername: String,
    displayName: String,
) : AndroidViewModel(application) {

    private val interactor = FriendsInteractor(
        application.applicationContext,
        currentUserId,
        funlifeUsername,
        displayName,
    )

    val linkState: StateFlow<com.example.funlife.social.model.SocialLinkState> =
        SocialSessionManager.linkState

    private val _uiState = MutableStateFlow<FriendsUiState>(
        FriendsUiState.Ready(friends = emptyList(), pendingIn = emptyList(), pendingOut = emptyList()),
    )
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _toast = MutableStateFlow<FriendsToast?>(null)
    val toast: StateFlow<FriendsToast?> = _toast.asStateFlow()

    private val _searchResult = MutableStateFlow<PbUserProfile?>(null)
    val searchResult: StateFlow<PbUserProfile?> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _sendingToPbId = MutableStateFlow<String?>(null)
    val sendingToPbId: StateFlow<String?> = _sendingToPbId.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingFriendAction?>(null)
    val pendingAction: StateFlow<PendingFriendAction?> = _pendingAction.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pbAuthToken = MutableStateFlow<String?>(null)
    val pbAuthToken: StateFlow<String?> = _pbAuthToken.asStateFlow()

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    private var searchJob: Job? = null

    init {
        if (!PocketBaseConfig.isEnabled()) {
            _uiState.value = FriendsUiState.Error(SocialFailure.NotConfigured.userMessage)
        } else {
            viewModelScope.launch {
                interactor.observeFriends().collect { items -> applyUiModels(items) }
            }
            viewModelScope.launch {
                SocialSessionManager.snapshot.collect {
                    refreshCredentialSnapshot()
                }
            }
            viewModelScope.launch {
                bootstrap(forceBind = false)
            }
        }
    }

    private suspend fun refreshCredentialSnapshot() {
        val cred = interactor.peekCredentials()
        _pbAuthToken.value = cred?.token
        _sessionReady.value = cred != null
    }

    private suspend fun bootstrap(forceBind: Boolean) {
        interactor.bootstrap(forceBind)
            .onFailure { e ->
                if (_uiState.value !is FriendsUiState.Ready) {
                    _uiState.value = FriendsUiState.Error(
                        SocialFailure.fromThrowable(e, "加载失败").userMessage,
                    )
                }
            }
        refreshCredentialSnapshot()
    }

    fun retryBootstrap() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                bootstrap(forceBind = true)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                interactor.syncFriends(forceSession = true)
                    .onFailure { showFailure(it) }
                refreshCredentialSnapshot()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun searchUser(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            _searchResult.value = null
            try {
                interactor.searchUser(query)
                    .onSuccess { profile ->
                        withContext(Dispatchers.Main) {
                            if (profile == null) {
                                val name = query.trim().removePrefix("@")
                                showToast("未找到 @$name", FriendsToastTone.Info)
                            } else {
                                _searchResult.value = profile
                            }
                        }
                    }
                    .onFailure { showFailure(it) }
                refreshCredentialSnapshot()
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun sendRequest(target: PbUserProfile) {
        if (_sendingToPbId.value != null) return
        viewModelScope.launch(Dispatchers.IO) {
            _sendingToPbId.value = target.id
            try {
                interactor.sendFriendRequest(target)
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            showToast("好友请求已发送", FriendsToastTone.Success)
                            _searchResult.value = null
                        }
                    }
                    .onFailure { showFailure(it) }
            } finally {
                _sendingToPbId.value = null
            }
        }
    }

    fun acceptRequest(friendshipId: String) {
        runFriendAction(PendingFriendActionKind.ACCEPT, friendshipId) {
            interactor.acceptRequest(friendshipId)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        showToast("已添加为好友", FriendsToastTone.Success)
                    }
                }
        }
    }

    fun rejectRequest(friendshipId: String) {
        runFriendAction(PendingFriendActionKind.REJECT, friendshipId) {
            interactor.rejectRequest(friendshipId)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        showToast("已拒绝好友申请", FriendsToastTone.Info)
                    }
                }
        }
    }

    fun removeFriend(friendshipId: String) {
        runFriendAction(PendingFriendActionKind.REJECT, friendshipId) {
            interactor.removeFriend(friendshipId)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        showToast("已删除好友", FriendsToastTone.Info)
                    }
                }
        }
    }

    fun updateRemark(friendPbId: String, remark: String) {
        viewModelScope.launch(Dispatchers.IO) {
            interactor.updateRemark(friendPbId, remark)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        showToast("备注已保存", FriendsToastTone.Success)
                    }
                }
                .onFailure { showFailure(it) }
        }
    }

    private fun runFriendAction(
        kind: PendingFriendActionKind,
        friendshipId: String,
        block: suspend () -> Result<*>,
    ) {
        if (_pendingAction.value != null) return
        viewModelScope.launch(Dispatchers.IO) {
            _pendingAction.value = PendingFriendAction(friendshipId, kind)
            try {
                block().onFailure { showFailure(it) }
            } finally {
                _pendingAction.value = null
            }
        }
    }

    private suspend fun showFailure(t: Throwable) {
        withContext(Dispatchers.Main) {
            val msg = SocialFailure.fromThrowable(t).userMessage
            showToast(msg, FriendsToastTone.Error)
        }
    }

    private fun applyUiModels(items: List<com.example.funlife.social.model.FriendUiModel>) {
        _uiState.value = FriendsUiState.Ready(
            friends = items.filter { it.status == FriendshipStatus.ACCEPTED },
            pendingIn = items.filter { it.isIncomingRequest },
            pendingOut = items.filter {
                !it.isIncomingRequest && it.status == FriendshipStatus.PENDING
            },
        )
    }

    private fun showToast(message: String, tone: FriendsToastTone) {
        _toast.value = FriendsToast(message, tone)
    }

    fun consumeToast() {
        _toast.value = null
    }
}
