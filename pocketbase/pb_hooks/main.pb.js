/// pb_hooks/main.pb.js
/// FunLife 企业级推送：friendships 新建 pending 时，向接收方 FCM Token 发推送。
///
/// 环境变量（PocketBase 启动前设置）：
///   FCM_RELAY_URL  — 推送中继 HTTP 地址（见 pocketbase/PUSH_SETUP.md）
///   FCM_RELAY_KEY  — 可选 Bearer Key
///
/// 无 FCM_RELAY_URL 时仅打日志，不影响好友业务。

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
            console.log("[push] skip: no fcm_token for addressee=" + addresseeId)
            e.next()
            return
        }

        var requesterName = requesterId
        if (requesterId) {
            var requester = $app.findRecordById("users", requesterId)
            if (requester) {
                requesterName = requester.getString("name") || requester.getString("funlife_username") || requesterId
            }
        }

        var relayUrl = $os.getenv("FCM_RELAY_URL")
        if (!relayUrl) {
            console.log("[push] FCM_RELAY_URL not set, would notify addressee=" + addresseeId)
            e.next()
            return
        }

        var relayKey = $os.getenv("FCM_RELAY_KEY") || ""
        var headers = { "Content-Type": "application/json" }
        if (relayKey) {
            headers["Authorization"] = "Bearer " + relayKey
        }

        var body = {
            token: fcmToken,
            title: "新的好友申请",
            body: requesterName + " 请求添加你为好友",
            data: {
                type: "friend_request",
                friendship_id: e.record.getId(),
                deep_link: "friends",
            },
        }

        var resp = $http.send({
            url: relayUrl,
            method: "POST",
            headers: headers,
            body: JSON.stringify(body),
            timeout: 10,
        })

        console.log("[push] relay status=" + resp.statusCode + " addressee=" + addresseeId)
    } catch (err) {
        console.log("[push] error: " + err)
    }

    e.next()
}, "friendships")
