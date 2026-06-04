/// pb_hooks/main.pb.js
/// FunLife 企业级推送：好友申请 + 私聊消息 → FCM（杀进程可达）
///
/// 环境变量（PocketBase 启动前设置）：
///   FCM_RELAY_URL  — 推送中继 HTTP 地址（见 pocketbase/PUSH_SETUP.md）
///   FCM_RELAY_KEY  — 可选 Bearer Key
///
/// 无 FCM_RELAY_URL 时仅打日志，不影响社交业务。

function sendFcmPush(fcmToken, title, body, data) {
    if (!fcmToken) {
        return { ok: false, reason: "no_token" }
    }
    var relayUrl = $os.getenv("FCM_RELAY_URL")
    if (!relayUrl) {
        console.log("[push] FCM_RELAY_URL not set, skip token=" + fcmToken.substring(0, 12) + "...")
        return { ok: false, reason: "no_relay" }
    }

    var relayKey = $os.getenv("FCM_RELAY_KEY") || ""
    var headers = { "Content-Type": "application/json" }
    if (relayKey) {
        headers["Authorization"] = "Bearer " + relayKey
    }

    var payload = {
        token: fcmToken,
        title: title,
        body: body,
        data: data || {},
    }

    var resp = $http.send({
        url: relayUrl,
        method: "POST",
        headers: headers,
        body: JSON.stringify(payload),
        timeout: 10,
    })

    return { ok: resp.statusCode >= 200 && resp.statusCode < 300, status: resp.statusCode }
}

function userDisplayName(userRecord, fallbackId) {
    if (!userRecord) return fallbackId || "用户"
    return userRecord.getString("name")
        || userRecord.getString("funlife_username")
        || fallbackId
        || "用户"
}

function previewBody(text, maxLen) {
    var s = (text || "").replace(/\s+/g, " ").trim()
    if (s.length <= maxLen) return s
    return s.substring(0, maxLen) + "…"
}

// ── 好友申请 pending ─────────────────────────────────────────────
onRecordAfterCreateSuccess((e) => {
    if (e.record.collection().name !== "friendships") {
        e.next()
        return
    }

    var status = e.record.getString("status")
    if (status !== "pending") {
        e.next()
        return
    }

    var addresseeId = e.record.getString("addressee")
    var requesterId = e.record.getString("requester")
    if (!addresseeId) {
        e.next()
        return
    }

    try {
        var addressee = $app.findRecordById("users", addresseeId)
        var fcmToken = addressee ? addressee.getString("fcm_token") : ""
        if (!fcmToken) {
            console.log("[push] friend_request skip: no fcm_token addressee=" + addresseeId)
            e.next()
            return
        }

        var requesterName = requesterId
        if (requesterId) {
            var requester = $app.findRecordById("users", requesterId)
            requesterName = userDisplayName(requester, requesterId)
        }

        var result = sendFcmPush(
            fcmToken,
            "新的好友申请",
            requesterName + " 请求添加你为好友",
            {
                type: "friend_request",
                friendship_id: e.record.getId(),
                deep_link: "friends",
            },
        )
        console.log("[push] friend_request status=" + (result.status || result.reason) + " addressee=" + addresseeId)
    } catch (err) {
        console.log("[push] friend_request error: " + err)
    }

    e.next()
}, "friendships")

// ── 私聊新消息 ───────────────────────────────────────────────────
onRecordAfterCreateSuccess((e) => {
    if (e.record.collection().name !== "messages") {
        e.next()
        return
    }

    var senderId = e.record.getString("sender")
    var memberA = e.record.getString("member_a")
    var memberB = e.record.getString("member_b")
    var conversationId = e.record.getString("conversation")
    var bodyText = e.record.getString("body") || ""

    if (!senderId || !memberA || !memberB) {
        e.next()
        return
    }

    var recipientId = senderId === memberA ? memberB : memberA
    if (!recipientId || recipientId === senderId) {
        e.next()
        return
    }

    try {
        var recipient = $app.findRecordById("users", recipientId)
        var fcmToken = recipient ? recipient.getString("fcm_token") : ""
        if (!fcmToken) {
            console.log("[push] chat skip: no fcm_token recipient=" + recipientId)
            e.next()
            return
        }

        var sender = $app.findRecordById("users", senderId)
        var senderName = userDisplayName(sender, senderId)
        var senderUsername = sender ? (sender.getString("funlife_username") || "") : ""
        var preview = previewBody(bodyText, 80)
        var createdAt = e.record.getDateTime("created")
        var createdMs = createdAt ? createdAt.getTime() : Date.now()

        var result = sendFcmPush(
            fcmToken,
            senderName,
            preview || "发来一条新消息",
            {
                type: "chat_message",
                message_id: e.record.getId(),
                conversation_id: conversationId,
                sender_pb_id: senderId,
                peer_pb_id: senderId,
                peer_display_name: senderName,
                peer_username: senderUsername,
                my_pb_id: recipientId,
                body: bodyText,
                created_at: String(createdMs),
                deep_link: "friend_chat/" + senderId,
            },
        )
        console.log("[push] chat status=" + (result.status || result.reason) + " recipient=" + recipientId)
    } catch (err) {
        console.log("[push] chat error: " + err)
    }

    e.next()
}, "messages")
