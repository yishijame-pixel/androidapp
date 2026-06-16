package com.example.funlife.social.game.model

/**
 * 大厅状态机守卫：所有 mutation 前置校验，避免非法转移。
 */
object GameRoomStateMachine {

    /** CAS 写入冲突：可安全重试 */
    class ConflictException(message: String = "房间状态已变化，请重试") : IllegalStateException(message)

    fun requireActiveRoom(dto: GameRoomDto) {
        if (dto.status !in GameRoomStatus.ACTIVE) {
            throw IllegalStateException("房间已结束，请重新开房间")
        }
    }

    fun requireHost(dto: GameRoomDto, myPbId: String) {
        if (dto.hostPbId != myPbId) {
            throw IllegalStateException("仅房主可操作")
        }
    }

    fun requireNotHost(dto: GameRoomDto, myPbId: String) {
        if (dto.hostPbId == myPbId) {
            throw IllegalStateException("房主请使用解散房间")
        }
    }

    fun requireMember(state: GameRoomStatePayload, myPbId: String) {
        if (!GameRoomStateCodec.isMember(state, myPbId)) {
            throw IllegalStateException("你不在这个房间内")
        }
    }

    fun requirePendingInviteFor(state: GameRoomStatePayload, myPbId: String) {
        val pending = state.pendingInvitePbId
        if (pending.isNullOrBlank() || pending != myPbId) {
            throw IllegalStateException("当前没有待处理的邀请")
        }
    }

    fun requireCanJoin(state: GameRoomStatePayload) {
        if (GameRoomStateCodec.joinedCount(state) >= state.maxPlayers) {
            throw IllegalStateException("房间已满")
        }
    }

    fun requireCanStart(state: GameRoomStatePayload) {
        val joined = GameRoomStateCodec.joinedCount(state)
        if (joined < state.minPlayers) {
            throw IllegalStateException("至少需要 ${state.minPlayers} 人才能开始")
        }
    }

    /** 开始游戏前：人数达标且无进行中的邀请 */
    fun requireReadyToStart(state: GameRoomStatePayload, gameType: String = "") {
        requireCanStart(state)
        if (!state.pendingInvitePbId.isNullOrBlank()) {
            throw IllegalStateException("仍有进行中的邀请，请稍后再试")
        }
        if (state.members.any { it.status == LobbyMemberStatus.PENDING.wire }) {
            throw IllegalStateException("仍有好友未回应邀请")
        }
        if (gameType == "pac_maze") {
            val pac = state.pacMaze
            if (pac == null || !pac.bothReady()) {
                throw IllegalStateException("双方准备就绪后才能开始")
            }
        }
    }

    fun requireCanAcceptInvite(state: GameRoomStatePayload, myPbId: String) {
        val hasPendingFlag = state.pendingInvitePbId == myPbId
        val hasPendingSeat = state.members.any {
            it.pbId == myPbId && it.status == LobbyMemberStatus.PENDING.wire
        }
        if (!hasPendingFlag && !hasPendingSeat) {
            throw IllegalStateException("当前没有待处理的邀请")
        }
    }

    fun requireRoomNotFull(state: GameRoomStatePayload) {
        if (GameRoomStateCodec.joinedCount(state) >= state.maxPlayers) {
            throw IllegalStateException("房间已满")
        }
    }
}
