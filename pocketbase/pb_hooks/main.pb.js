/// pb_hooks/main.pb.js
/// FunLife 企业级推送：好友申请 + 私聊消息 → FCM（杀进程可达）

onBootstrap((e) => {
    console.log("[hooks] FunLife pb_hooks loaded (draw_guess authority v2)")
    e.next()
})
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

/** PB 0.39+ record 用 .id，旧版用 getId() */
function recordId(record) {
    if (!record) return ""
    if (typeof record.getId === "function") {
        try {
            var gid = record.getId()
            if (gid) return String(gid).trim()
        } catch (e1) { /* noop */ }
    }
    if (record.id) return String(record.id).trim()
    try {
        var sid = record.getString("id")
        if (sid) return sid.trim()
    } catch (e2) { /* noop */ }
    return ""
}

// ── 游戏逻辑辅助（必须在首个 onRecord* 之前，否则 PB 隔离作用域找不到）──
var GOMOKU_SIZE = 15
var GOMOKU_EMPTY = "."
var DRAW_GUESS_WORDS = [
    "苹果", "香蕉", "火锅", "蛋糕", "咖啡",
    "猫咪", "小狗", "熊猫", "老虎", "兔子",
    "太阳", "月亮", "彩虹", "雨伞", "雪花",
    "篮球", "足球", "游泳", "跑步", "自行车",
    "手机", "电脑", "电视", "相机", "耳机",
    "医生", "警察", "厨师", "老师", "画家",
    "长城", "金字塔", "埃菲尔铁塔", "自由女神", "故宫",
    "守株待兔", "画蛇添足", "对牛弹琴", "亡羊补牢", "掩耳盗铃",
    "泰坦尼克", "哈利波特", "蜘蛛侠", "皮卡丘", "米老鼠",
]

function utf8FromBytes(bytes) {
    if (typeof TextDecoder !== "undefined") {
        return new TextDecoder("utf-8").decode(new Uint8Array(bytes))
    }
    var s = ""
    for (var i = 0; i < bytes.length; ) {
        var b0 = bytes[i++]
        if (b0 < 0x80) { s += String.fromCharCode(b0); continue }
        var b1 = bytes[i++] & 0x3F
        if ((b0 & 0xE0) === 0xC0) {
            s += String.fromCharCode(((b0 & 0x1F) << 6) | b1)
            continue
        }
        var b2 = bytes[i++] & 0x3F
        if ((b0 & 0xF0) === 0xE0) {
            s += String.fromCharCode(((b0 & 0x0F) << 12) | (b1 << 6) | b2)
            continue
        }
        var b3 = bytes[i++] & 0x3F
        s += String.fromCharCode(((b0 & 0x07) << 18) | (b1 << 12) | (b2 << 6) | b3)
    }
    return s
}

function parseJsonField(raw) {
    if (!raw) return null
    if (Array.isArray(raw)) {
        try { return JSON.parse(utf8FromBytes(raw)) } catch (e1) { return null }
    }
    if (typeof raw === "string") {
        try { return JSON.parse(raw) } catch (e2) { return null }
    }
    if (typeof raw === "object") return raw
    return null
}

function readMovePayload(record) {
    return parseJsonField(record.get("payload"))
}

function readGameState(room) {
    return parseJsonField(room.get("game_state")) || {}
}

function gomokuEmptyBoard() {
    var s = ""
    for (var i = 0; i < GOMOKU_SIZE * GOMOKU_SIZE; i++) s += GOMOKU_EMPTY
    return s
}

function gomokuCell(board, x, y) {
    if (x < 0 || x >= GOMOKU_SIZE || y < 0 || y >= GOMOKU_SIZE) return GOMOKU_EMPTY
    return board.charAt(y * GOMOKU_SIZE + x) || GOMOKU_EMPTY
}

function gomokuApplyMove(board, x, y, color) {
    var chars = board.split("")
    chars[y * GOMOKU_SIZE + x] = color
    return chars.join("")
}

function gomokuColorForPb(black, white, pbId) {
    if (pbId === black) return "B"
    if (pbId === white) return "W"
    return null
}

function gomokuPbForColor(black, white, color) {
    if (color === "B") return black
    if (color === "W") return white
    return ""
}

function applyGomokuMoveOnRoom(room, playerId, x, y) {
    if (!room || room.getString("game_type") !== "gomoku") return
    if (room.getString("status") !== "playing") return
    var gs = readGameState(room)
    var gomoku = gs.gomoku
    if (!gomoku || typeof gomoku !== "object") gomoku = {}
    var black = (gomoku.black_pb_id || room.getString("host") || "").trim()
    var white = (gomoku.white_pb_id || room.getString("guest") || "").trim()
    if (!black || !white) return
    var board = gomoku.board || gomokuEmptyBoard()
    if (board.length !== GOMOKU_SIZE * GOMOKU_SIZE) board = gomokuEmptyBoard()
    var color = gomokuColorForPb(black, white, playerId)
    if (!color || gomokuCell(board, x, y) !== GOMOKU_EMPTY) return
    board = gomokuApplyMove(board, x, y, color)
    var nextColor = color === "B" ? "W" : "B"
    var nextTurn = gomokuPbForColor(black, white, nextColor)
    gomoku.board = board
    gomoku.move_count = (gomoku.move_count || 0) + 1
    gomoku.black_pb_id = black
    gomoku.white_pb_id = white
    gomoku.last_move = { x: x, y: y, color: color }
    gs.gomoku = gomoku
    room.set("game_state", gs)
    if (nextTurn) room.set("current_turn", nextTurn)
    $app.save(room)
}

function normalizeDrawGuess(text) {
    return (text || "").trim().toLowerCase().replace(/\s+/g, "")
}

function pickDrawGuessWord(usedWords) {
    var used = usedWords || []
    var pool = DRAW_GUESS_WORDS.filter(function (w) { return used.indexOf(w) < 0 })
    if (pool.length === 0) pool = DRAW_GUESS_WORDS
    return pool[Math.floor(Math.random() * pool.length)]
}

function drawGuessGuesserPbId(room, drawerPbId) {
    var host = (room.getString("host") || "").trim()
    var guest = (room.getString("guest") || "").trim()
    var drawer = (drawerPbId || "").trim()
    if (!drawer) return ""
    return drawer === host ? guest : host
}

function redactDrawGuessWord(room, viewerId) {
    if (!room || room.getString("game_type") !== "draw_guess") return
    var gs = readGameState(room)
    if (!gs || typeof gs !== "object") return
    var dg = gs.draw_guess
    if (!dg || typeof dg !== "object") return
    var phase = dg.phase || ""
    if (phase === "round_end" || phase === "finished") return
    var drawer = (dg.drawer_pb_id || "").trim()
    if ((viewerId || "").trim() !== drawer) {
        dg.word = ""
        gs.draw_guess = dg
        room.set("game_state", gs)
    }
}

function applyDrawStrokeOnRoom(room, playerId, seq) {
    var gs = readGameState(room)
    var dg = gs.draw_guess
    if (!dg || typeof dg !== "object") return
    if ((dg.drawer_pb_id || "").trim() !== (playerId || "").trim()) return
    if ((dg.phase || "") !== "drawing") return
    dg.stroke_seq = Math.max(dg.stroke_seq || 0, seq || 0)
    gs.draw_guess = dg
    room.set("game_state", gs)
    $app.save(room)
}

function applyDrawPhaseOnRoom(room, playerId, phase, round) {
    if (!phase) return
    var gs = readGameState(room)
    var dg = gs.draw_guess
    if (!dg || typeof dg !== "object") return
    var now = Date.now()
    var prevPhase = dg.phase || "drawing"
    var drawer = (dg.drawer_pb_id || "").trim()
    var host = (room.getString("host") || "").trim()
    var guest = (room.getString("guest") || "").trim()
    if (phase === "guessing") {
        if (prevPhase !== "drawing") return
        if ((playerId || "").trim() !== drawer) {
            var started = dg.phase_started_at_ms || 0
            var drawSec = dg.draw_seconds || 60
            if (started <= 0 || (now - started) < drawSec * 1000) return
        }
        dg.phase = "guessing"
        dg.guesses = []
        dg.phase_started_at_ms = now
    } else if (phase === "drawing") {
        if (round !== undefined && round !== null && round > (dg.round || 1)) {
            var used = dg.used_words || []
            if (dg.word) used = used.concat([dg.word])
            dg.round = round
            dg.drawer_pb_id = drawer === host ? guest : host
            dg.word = pickDrawGuessWord(used)
            dg.used_words = used
            dg.guesses = []
            dg.stroke_seq = 0
        }
        dg.phase = "drawing"
        dg.phase_started_at_ms = now
    } else if (phase === "round_end") {
        if (prevPhase !== "guessing") return
        var guessStarted = dg.phase_started_at_ms || 0
        var guessSec = dg.guess_seconds || 90
        var limit = dg.guess_limit || 5
        var guesses = dg.guesses || []
        var guesser = drawGuessGuesserPbId(room, drawer)
        var guesserCount = 0
        for (var gi = 0; gi < guesses.length; gi++) {
            if (guesses[gi].pb_id === guesser) guesserCount++
        }
        var timedOut = guessStarted > 0 && (now - guessStarted) >= guessSec * 1000
        if (!timedOut && guesserCount < limit) return
        dg.phase = "round_end"
        dg.phase_started_at_ms = now
    } else {
        dg.phase = phase
        if (round !== undefined && round !== null) dg.round = round
        dg.phase_started_at_ms = now
    }
    gs.draw_guess = dg
    room.set("game_state", gs)
    if (phase === "drawing" && dg.drawer_pb_id) {
        room.set("current_turn", dg.drawer_pb_id)
    } else if (phase === "guessing") {
        var guesserTurn = drawGuessGuesserPbId(room, dg.drawer_pb_id)
        if (guesserTurn) room.set("current_turn", guesserTurn)
    } else if (phase === "round_end") {
        if (drawer) room.set("current_turn", drawer)
    } else if (phase === "finished") {
        room.set("status", "finished")
        var scores = dg.scores || {}
        var top = -1
        var leaders = []
        for (var pid in scores) {
            if (!scores.hasOwnProperty(pid)) continue
            var sc = scores[pid] || 0
            if (sc > top) { top = sc; leaders = [pid] }
            else if (sc === top) { leaders.push(pid) }
        }
        if (leaders.length === 1) room.set("winner", leaders[0])
    }
    $app.save(room)
}

function applyDrawGuessOnRoom(room, playerId, text, round) {
    var gs = readGameState(room)
    var dg = gs.draw_guess
    if (!dg || typeof dg !== "object") return
    if ((dg.phase || "") !== "guessing") return
    var drawer = (dg.drawer_pb_id || "").trim()
    if ((playerId || "").trim() === drawer) return
    var moveRound = round !== undefined && round !== null ? round : (dg.round || 1)
    if (moveRound !== (dg.round || 1)) return
    var guesses = dg.guesses || []
    var normalized = normalizeDrawGuess(text)
    if (!normalized) return
    for (var i = 0; i < guesses.length; i++) {
        var g = guesses[i]
        if (g.pb_id === playerId && normalizeDrawGuess(g.text) === normalized) return
    }
    var limit = dg.guess_limit || 5
    var playerCount = 0
    for (var j = 0; j < guesses.length; j++) {
        if (guesses[j].pb_id === playerId) playerCount++
    }
    if (playerCount >= limit) return
    var correct = normalized === normalizeDrawGuess(dg.word)
    guesses.push({ pb_id: playerId, text: (text || "").trim(), correct: correct })
    dg.guesses = guesses
    var scores = dg.scores || {}
    if (correct) {
        scores[playerId] = (scores[playerId] || 0) + 1
        dg.scores = scores
        if ((dg.round || 1) >= (dg.max_rounds || 3)) {
            dg.phase = "finished"
            dg.phase_started_at_ms = Date.now()
            gs.draw_guess = dg
            room.set("game_state", gs)
            room.set("status", "finished")
            room.set("winner", playerId)
            $app.save(room)
            return
        }
        dg.phase = "round_end"
        dg.phase_started_at_ms = Date.now()
        if (drawer) room.set("current_turn", drawer)
    } else {
        playerCount++
        if (playerCount >= limit) {
            dg.phase = "round_end"
            dg.phase_started_at_ms = Date.now()
            if (drawer) room.set("current_turn", drawer)
        }
    }
    gs.draw_guess = dg
    room.set("game_state", gs)
    $app.save(room)
}

// ── game_moves：五子棋 / 你画我猜 服务端权威同步 ──
onRecordAfterCreateSuccess((e) => {
    function parseJsonFieldLocal(raw) {
        if (!raw) return null
        if (Array.isArray(raw)) {
            var text = ""
            if (typeof TextDecoder !== "undefined") {
                text = new TextDecoder("utf-8").decode(new Uint8Array(raw))
            } else {
                for (var ui = 0; ui < raw.length; ) {
                    var b0 = raw[ui++]
                    if (b0 < 0x80) { text += String.fromCharCode(b0); continue }
                    var b1 = raw[ui++] & 0x3F
                    if ((b0 & 0xE0) === 0xC0) {
                        text += String.fromCharCode(((b0 & 0x1F) << 6) | b1)
                        continue
                    }
                    var b2 = raw[ui++] & 0x3F
                    text += String.fromCharCode(((b0 & 0x0F) << 12) | (b1 << 6) | b2)
                }
            }
            try { return JSON.parse(text) } catch (pe) { return null }
        }
        if (typeof raw === "string") {
            try { return JSON.parse(raw) } catch (pe2) { return null }
        }
        if (typeof raw === "object") return raw
        return null
    }
    function normalizeDrawGuessLocal(text) {
        return (text || "").trim().toLowerCase().replace(/\s+/g, "")
    }
    function guesserPbIdLocal(roomRec, drawerPbId) {
        var host = (roomRec.getString("host") || "").trim()
        var guest = (roomRec.getString("guest") || "").trim()
        var drawer = (drawerPbId || "").trim()
        return drawer === host ? guest : host
    }
    function pickWordLocal(usedWords) {
        var bank = [
            "苹果", "香蕉", "火锅", "蛋糕", "咖啡", "猫咪", "小狗", "熊猫", "老虎", "兔子",
            "太阳", "月亮", "彩虹", "雨伞", "雪花", "篮球", "足球", "游泳", "跑步", "自行车",
        ]
        var used = usedWords || []
        var pool = bank.filter(function (w) { return used.indexOf(w) < 0 })
        if (pool.length === 0) pool = bank
        return pool[Math.floor(Math.random() * pool.length)]
    }
    try {
        var payload = parseJsonFieldLocal(e.record.get("payload"))
        if (!payload || typeof payload !== "object" || Array.isArray(payload)) return
        var roomId = e.record.getString("room")
        var playerId = e.record.getString("player")
        if (!roomId || !playerId) return
        var room = $app.findRecordById("game_rooms", roomId)
        if (!room) return

        if (payload.kind === "gomoku_place") {
            var x = payload.x
            var y = payload.y
            if (x === undefined || y === undefined) return
            applyGomokuMoveOnRoom(room, playerId, x, y)
            return
        }

        if (room.getString("game_type") !== "draw_guess") return
        var gs = parseJsonFieldLocal(room.get("game_state")) || {}
        var dg = gs.draw_guess
        if (!dg || typeof dg !== "object") return

        if (payload.kind === "draw_stroke") {
            if ((dg.drawer_pb_id || "").trim() === (playerId || "").trim() &&
                (dg.phase || "") === "drawing") {
                dg.stroke_seq = Math.max(dg.stroke_seq || 0, payload.seq || 0)
                gs.draw_guess = dg
                room.set("game_state", gs)
                $app.save(room)
            }
        } else if (payload.kind === "draw_phase") {
            var phase = payload.phase
            var round = payload.round
            if (!phase) return
            var now = Date.now()
            var prevPhase = dg.phase || "drawing"
            var drawer = (dg.drawer_pb_id || "").trim()
            var host = (room.getString("host") || "").trim()
            var guest = (room.getString("guest") || "").trim()
            if (phase === "guessing") {
                if (prevPhase !== "drawing") return
                if ((playerId || "").trim() !== drawer) {
                    var started = dg.phase_started_at_ms || 0
                    var drawSec = dg.draw_seconds || 60
                    if (started <= 0 || (now - started) < drawSec * 1000) return
                }
                dg.phase = "guessing"
                dg.guesses = []
                dg.phase_started_at_ms = now
            } else if (phase === "drawing") {
                if (round !== undefined && round !== null && round > (dg.round || 1)) {
                    var used = dg.used_words || []
                    if (dg.word) used = used.concat([dg.word])
                    dg.round = round
                    dg.drawer_pb_id = drawer === host ? guest : host
                    dg.word = pickWordLocal(used)
                    dg.used_words = used
                    dg.guesses = []
                    dg.stroke_seq = 0
                }
                dg.phase = "drawing"
                dg.phase_started_at_ms = now
            } else if (phase === "round_end") {
                if (prevPhase !== "guessing") return
                var guessStarted = dg.phase_started_at_ms || 0
                var guessSec = dg.guess_seconds || 90
                var limit = dg.guess_limit || 5
                var guesses = dg.guesses || []
                var guesser = guesserPbIdLocal(room, drawer)
                var guesserCount = 0
                for (var gi = 0; gi < guesses.length; gi++) {
                    if (guesses[gi].pb_id === guesser) guesserCount++
                }
                var timedOut = guessStarted > 0 && (now - guessStarted) >= guessSec * 1000
                if (!timedOut && guesserCount < limit) return
                dg.phase = "round_end"
                dg.phase_started_at_ms = now
            } else {
                dg.phase = phase
                if (round !== undefined && round !== null) dg.round = round
                dg.phase_started_at_ms = now
            }
            gs.draw_guess = dg
            room.set("game_state", gs)
            if (phase === "drawing" && dg.drawer_pb_id) {
                room.set("current_turn", dg.drawer_pb_id)
            } else if (phase === "guessing") {
                var guesserTurn = guesserPbIdLocal(room, dg.drawer_pb_id)
                if (guesserTurn) room.set("current_turn", guesserTurn)
            } else if (phase === "round_end" && drawer) {
                room.set("current_turn", drawer)
            }
            $app.save(room)
        } else if (payload.kind === "draw_guess") {
            if ((dg.phase || "") !== "guessing") return
            if ((playerId || "").trim() === (dg.drawer_pb_id || "").trim()) return
            var moveRound = payload.round !== undefined && payload.round !== null ? payload.round : (dg.round || 1)
            if (moveRound !== (dg.round || 1)) return
            var guessesList = dg.guesses || []
            var normalized = normalizeDrawGuessLocal(payload.text || "")
            if (!normalized) return
            for (var i = 0; i < guessesList.length; i++) {
                var g = guessesList[i]
                if (g.pb_id === playerId && normalizeDrawGuessLocal(g.text) === normalized) return
            }
            var guessLimit = dg.guess_limit || 5
            var playerCount = 0
            for (var j = 0; j < guessesList.length; j++) {
                if (guessesList[j].pb_id === playerId) playerCount++
            }
            if (playerCount >= guessLimit) return
            var correct = normalized === normalizeDrawGuessLocal(dg.word)
            guessesList.push({ pb_id: playerId, text: (payload.text || "").trim(), correct: correct })
            dg.guesses = guessesList
            var scores = dg.scores || {}
            if (correct) {
                scores[playerId] = (scores[playerId] || 0) + 1
                dg.scores = scores
                if ((dg.round || 1) >= (dg.max_rounds || 3)) {
                    dg.phase = "finished"
                    dg.phase_started_at_ms = Date.now()
                    gs.draw_guess = dg
                    room.set("game_state", gs)
                    room.set("status", "finished")
                    room.set("winner", playerId)
                    $app.save(room)
                    return
                }
                dg.phase = "round_end"
                dg.phase_started_at_ms = Date.now()
                if ((dg.drawer_pb_id || "").trim()) room.set("current_turn", dg.drawer_pb_id)
            } else {
                playerCount++
                if (playerCount >= guessLimit) {
                    dg.phase = "round_end"
                    dg.phase_started_at_ms = Date.now()
                    if ((dg.drawer_pb_id || "").trim()) room.set("current_turn", dg.drawer_pb_id)
                }
            }
            gs.draw_guess = dg
            room.set("game_state", gs)
            $app.save(room)
        }
    } catch (err) {
        console.log("[game_moves] hook error: " + err)
    } finally {
        e.next()
    }
}, "game_moves")

// ── 你画我猜：词语脱敏（猜词方不可读 word）────────────────────────
onRecordEnrich((e) => {
    function parseJsonFieldLocal(raw) {
        if (!raw) return null
        if (Array.isArray(raw)) {
            var text = ""
            if (typeof TextDecoder !== "undefined") {
                text = new TextDecoder("utf-8").decode(new Uint8Array(raw))
            } else {
                for (var ui = 0; ui < raw.length; ) {
                    var b0 = raw[ui++]
                    if (b0 < 0x80) { text += String.fromCharCode(b0); continue }
                    var b1 = raw[ui++] & 0x3F
                    if ((b0 & 0xE0) === 0xC0) {
                        text += String.fromCharCode(((b0 & 0x1F) << 6) | b1)
                        continue
                    }
                    var b2 = raw[ui++] & 0x3F
                    text += String.fromCharCode(((b0 & 0x0F) << 12) | (b1 << 6) | b2)
                }
            }
            try { return JSON.parse(text) } catch (pe) { return null }
        }
        if (typeof raw === "string") {
            try { return JSON.parse(raw) } catch (pe2) { return null }
        }
        if (typeof raw === "object") return raw
        return null
    }
    try {
        if (e.record.getString("game_type") !== "draw_guess") return
        var viewerId = ""
        if (e.requestInfo && e.requestInfo.auth) {
            viewerId = (e.requestInfo.auth.get("id") || e.requestInfo.auth.id || "").trim()
        }
        var gs = parseJsonFieldLocal(e.record.get("game_state"))
        if (!gs || typeof gs !== "object") return
        var dg = gs.draw_guess
        if (!dg || typeof dg !== "object") return
        var phase = dg.phase || ""
        if (phase === "round_end" || phase === "finished") return
        var drawer = (dg.drawer_pb_id || "").trim()
        if ((viewerId || "").trim() !== drawer) {
            dg.word = ""
            gs.draw_guess = dg
            e.record.set("game_state", gs)
        }
    } catch (err) {
        console.log("[draw_guess] enrich redact error: " + err)
    }
    e.next()
}, "game_rooms")

// ── 推送 hooks（工具包 pushHookKit 定义在首个 onRecord* 之前）────────
// ── 好友申请 pending ─────────────────────────────────────────────
onRecordAfterCreateSuccess((e) => {
    var kit = (function () {
        function pbRecordId(rec) {
            if (!rec) return ""
            if (rec.id) return String(rec.id).trim()
            try {
                var sid = rec.getString("id")
                if (sid) return String(sid).trim()
            } catch (ignore) { /* noop */ }
            return ""
        }
        function pushUserDisplayName(userRecord, fallbackId) {
            if (!userRecord) return fallbackId || "用户"
            return userRecord.getString("name")
                || userRecord.getString("funlife_username")
                || fallbackId
                || "用户"
        }
        function pushPreviewBody(text, maxLen) {
            var s = (text || "").replace(/\s+/g, " ").trim()
            if (s.length <= maxLen) return s
            return s.substring(0, maxLen) + "…"
        }
        function pushSendFcm(fcmToken, title, body, data) {
            if (!fcmToken) return { ok: false, reason: "no_token" }
            var pushType = (data && data.type) ? data.type : "unknown"
            var relayUrl = $os.getenv("FCM_RELAY_URL")
            if (!relayUrl) {
                console.log("[push] " + pushType + " skip: no_relay")
                return { ok: false, reason: "no_relay" }
            }
            var relayKey = $os.getenv("FCM_RELAY_KEY") || ""
            var headers = { "Content-Type": "application/json" }
            if (relayKey) headers["Authorization"] = "Bearer " + relayKey
            try {
                var resp = $http.send({
                    url: relayUrl,
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({ token: fcmToken, title: title, body: body, data: data || {} }),
                    timeout: 3,
                })
                var respBody = null
                try {
                    if (resp.json) respBody = resp.json
                    else if (resp.raw) respBody = JSON.parse(resp.raw)
                } catch (parseErr) { /* noop */ }
                if (resp.statusCode >= 200 && resp.statusCode < 300) {
                    if (respBody && respBody.ok === true) {
                        return { ok: true, status: resp.statusCode }
                    }
                    var why = (respBody && respBody.error) ? respBody.error : "relay_declined"
                    console.log("[push] " + pushType + " skip: " + why)
                    return { ok: false, reason: why }
                }
                console.log("[push] " + pushType + " skip: relay_http_" + resp.statusCode)
                return { ok: false, status: resp.statusCode, reason: "relay_http" }
            } catch (httpErr) {
                console.log("[push] " + pushType + " skip: relay_timeout")
                return { ok: false, reason: "relay_timeout" }
            }
        }
        return { pbRecordId: pbRecordId, pushUserDisplayName: pushUserDisplayName, pushPreviewBody: pushPreviewBody, pushSendFcm: pushSendFcm }
    })()
    try {
        var status = e.record.getString("status")
        if (status !== "pending") return

        var addresseeId = e.record.getString("addressee")
        var requesterId = e.record.getString("requester")
        if (!addresseeId) return

        var addressee = $app.findRecordById("users", addresseeId)
        var fcmToken = addressee ? addressee.getString("fcm_token") : ""
        if (!fcmToken) {
            console.log("[push] friend_request skip: no fcm_token addressee=" + addresseeId)
            return
        }

        var requesterName = requesterId
        if (requesterId) {
            var requester = $app.findRecordById("users", requesterId)
            requesterName = kit.pushUserDisplayName(requester, requesterId)
        }

        var result = kit.pushSendFcm(
            fcmToken,
            "新的好友申请",
            requesterName + " 请求添加你为好友",
            {
                type: "friend_request",
                friendship_id: kit.pbRecordId(e.record),
                deep_link: "friends",
            },
        )
        if (result.ok) {
            console.log("[push] friend_request sent addressee=" + addresseeId)
        }
    } catch (err) {
        console.log("[push] friend_request error: " + err)
    } finally {
        e.next()
    }
}, "friendships")

// ── 私聊新消息 ───────────────────────────────────────────────────
onRecordAfterCreateSuccess((e) => {
    var kit = (function () {
        function pbRecordId(rec) {
            if (!rec) return ""
            if (rec.id) return String(rec.id).trim()
            try {
                var sid = rec.getString("id")
                if (sid) return String(sid).trim()
            } catch (ignore) { /* noop */ }
            return ""
        }
        function pushUserDisplayName(userRecord, fallbackId) {
            if (!userRecord) return fallbackId || "用户"
            return userRecord.getString("name")
                || userRecord.getString("funlife_username")
                || fallbackId
                || "用户"
        }
        function pushPreviewBody(text, maxLen) {
            var s = (text || "").replace(/\s+/g, " ").trim()
            if (s.length <= maxLen) return s
            return s.substring(0, maxLen) + "…"
        }
        function pushSendFcm(fcmToken, title, body, data) {
            if (!fcmToken) return { ok: false, reason: "no_token" }
            var pushType = (data && data.type) ? data.type : "unknown"
            var relayUrl = $os.getenv("FCM_RELAY_URL")
            if (!relayUrl) {
                console.log("[push] " + pushType + " skip: no_relay")
                return { ok: false, reason: "no_relay" }
            }
            var relayKey = $os.getenv("FCM_RELAY_KEY") || ""
            var headers = { "Content-Type": "application/json" }
            if (relayKey) headers["Authorization"] = "Bearer " + relayKey
            try {
                var resp = $http.send({
                    url: relayUrl,
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({ token: fcmToken, title: title, body: body, data: data || {} }),
                    timeout: 3,
                })
                var respBody = null
                try {
                    if (resp.json) respBody = resp.json
                    else if (resp.raw) respBody = JSON.parse(resp.raw)
                } catch (parseErr) { /* noop */ }
                if (resp.statusCode >= 200 && resp.statusCode < 300) {
                    if (respBody && respBody.ok === true) {
                        return { ok: true, status: resp.statusCode }
                    }
                    var why = (respBody && respBody.error) ? respBody.error : "relay_declined"
                    console.log("[push] " + pushType + " skip: " + why)
                    return { ok: false, reason: why }
                }
                console.log("[push] " + pushType + " skip: relay_http_" + resp.statusCode)
                return { ok: false, status: resp.statusCode, reason: "relay_http" }
            } catch (httpErr) {
                console.log("[push] " + pushType + " skip: relay_timeout")
                return { ok: false, reason: "relay_timeout" }
            }
        }
        return { pbRecordId: pbRecordId, pushUserDisplayName: pushUserDisplayName, pushPreviewBody: pushPreviewBody, pushSendFcm: pushSendFcm }
    })()
    try {
        var senderId = e.record.getString("sender")
        var memberA = e.record.getString("member_a")
        var memberB = e.record.getString("member_b")
        var conversationId = e.record.getString("conversation")
        var bodyText = e.record.getString("body") || ""

        if (!senderId || !memberA || !memberB) return

        var recipientId = senderId === memberA ? memberB : memberA
        if (!recipientId || recipientId === senderId) return

        var recipient = $app.findRecordById("users", recipientId)
        var fcmToken = recipient ? recipient.getString("fcm_token") : ""
        if (!fcmToken) {
            console.log("[push] chat skip: no fcm_token recipient=" + recipientId)
            return
        }

        var sender = $app.findRecordById("users", senderId)
        var senderName = kit.pushUserDisplayName(sender, senderId)
        var senderUsername = sender ? (sender.getString("funlife_username") || "") : ""
        var preview = kit.pushPreviewBody(bodyText, 80)
        var createdAt = e.record.getDateTime("created")
        var createdMs = createdAt ? createdAt.getTime() : Date.now()

        var result = kit.pushSendFcm(
            fcmToken,
            senderName,
            preview || "发来一条新消息",
            {
                type: "chat_message",
                message_id: kit.pbRecordId(e.record),
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
        if (result.ok) {
            console.log("[push] chat sent recipient=" + recipientId)
        }
    } catch (err) {
        console.log("[push] chat error: " + err)
    } finally {
        e.next()
    }
}, "messages")

// ── 游戏邀请（direct + waiting + 已指定 guest 时推 FCM）──
onRecordAfterCreateSuccess((e) => {
    var kit = (function () {
        function pbRecordId(rec) {
            if (!rec) return ""
            if (rec.id) return String(rec.id).trim()
            try {
                var sid = rec.getString("id")
                if (sid) return String(sid).trim()
            } catch (ignore) { /* noop */ }
            return ""
        }
        function pushUserDisplayName(userRecord, fallbackId) {
            if (!userRecord) return fallbackId || "用户"
            return userRecord.getString("name")
                || userRecord.getString("funlife_username")
                || fallbackId
                || "用户"
        }
        function pushPreviewBody(text, maxLen) {
            var s = (text || "").replace(/\s+/g, " ").trim()
            if (s.length <= maxLen) return s
            return s.substring(0, maxLen) + "…"
        }
        function pushSendFcm(fcmToken, title, body, data) {
            if (!fcmToken) return { ok: false, reason: "no_token" }
            var pushType = (data && data.type) ? data.type : "unknown"
            var relayUrl = $os.getenv("FCM_RELAY_URL")
            if (!relayUrl) {
                console.log("[push] " + pushType + " skip: no_relay")
                return { ok: false, reason: "no_relay" }
            }
            var relayKey = $os.getenv("FCM_RELAY_KEY") || ""
            var headers = { "Content-Type": "application/json" }
            if (relayKey) headers["Authorization"] = "Bearer " + relayKey
            try {
                var resp = $http.send({
                    url: relayUrl,
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({ token: fcmToken, title: title, body: body, data: data || {} }),
                    timeout: 3,
                })
                var respBody = null
                try {
                    if (resp.json) respBody = resp.json
                    else if (resp.raw) respBody = JSON.parse(resp.raw)
                } catch (parseErr) { /* noop */ }
                if (resp.statusCode >= 200 && resp.statusCode < 300) {
                    if (respBody && respBody.ok === true) {
                        return { ok: true, status: resp.statusCode }
                    }
                    var why = (respBody && respBody.error) ? respBody.error : "relay_declined"
                    console.log("[push] " + pushType + " skip: " + why)
                    return { ok: false, reason: why }
                }
                console.log("[push] " + pushType + " skip: relay_http_" + resp.statusCode)
                return { ok: false, status: resp.statusCode, reason: "relay_http" }
            } catch (httpErr) {
                console.log("[push] " + pushType + " skip: relay_timeout")
                return { ok: false, reason: "relay_timeout" }
            }
        }
        return { pbRecordId: pbRecordId, pushUserDisplayName: pushUserDisplayName, pushPreviewBody: pushPreviewBody, pushSendFcm: pushSendFcm }
    })()
    try {
        var record = e.record
        if (record.getString("invite_mode") !== "direct") return
        if (record.getString("status") !== "waiting") return

        var hostId = record.getString("host")
        var guestId = record.getString("guest")
        var roomId = kit.pbRecordId(record)
        if (!hostId || !guestId || !roomId) return

        var host = $app.findRecordById("users", hostId)
        var hostName = kit.pushUserDisplayName(host, hostId)
        var guest = $app.findRecordById("users", guestId)
        var fcmToken = guest ? guest.getString("fcm_token") : ""
        if (!fcmToken) {
            console.log("[push] game_invite skip: no fcm_token guest=" + guestId)
            return
        }
        var gameType = record.getString("game_type") || "game"
        var result = kit.pushSendFcm(
            fcmToken,
            hostName + " 邀请你一起玩",
            gameType + " · 点击接受",
            {
                type: "game_invite",
                room_id: roomId,
                room_code: record.getString("room_code") || "",
                game_type: gameType,
                host_pb_id: hostId,
                deep_link: "social_game_lobby/" + roomId,
            },
        )
        if (result.ok) {
            console.log("[push] game_invite sent room=" + roomId)
        }
    } catch (err) {
        console.log("[push] game_invite unexpected: " + err)
    } finally {
        e.next()
    }
}, "game_rooms")

onRecordAfterUpdateSuccess((e) => {
    var kit = (function () {
        function pbRecordId(rec) {
            if (!rec) return ""
            if (rec.id) return String(rec.id).trim()
            try {
                var sid = rec.getString("id")
                if (sid) return String(sid).trim()
            } catch (ignore) { /* noop */ }
            return ""
        }
        function pushUserDisplayName(userRecord, fallbackId) {
            if (!userRecord) return fallbackId || "用户"
            return userRecord.getString("name")
                || userRecord.getString("funlife_username")
                || fallbackId
                || "用户"
        }
        function pushPreviewBody(text, maxLen) {
            var s = (text || "").replace(/\s+/g, " ").trim()
            if (s.length <= maxLen) return s
            return s.substring(0, maxLen) + "…"
        }
        function pushSendFcm(fcmToken, title, body, data) {
            if (!fcmToken) return { ok: false, reason: "no_token" }
            var pushType = (data && data.type) ? data.type : "unknown"
            var relayUrl = $os.getenv("FCM_RELAY_URL")
            if (!relayUrl) {
                console.log("[push] " + pushType + " skip: no_relay")
                return { ok: false, reason: "no_relay" }
            }
            var relayKey = $os.getenv("FCM_RELAY_KEY") || ""
            var headers = { "Content-Type": "application/json" }
            if (relayKey) headers["Authorization"] = "Bearer " + relayKey
            try {
                var resp = $http.send({
                    url: relayUrl,
                    method: "POST",
                    headers: headers,
                    body: JSON.stringify({ token: fcmToken, title: title, body: body, data: data || {} }),
                    timeout: 3,
                })
                var respBody = null
                try {
                    if (resp.json) respBody = resp.json
                    else if (resp.raw) respBody = JSON.parse(resp.raw)
                } catch (parseErr) { /* noop */ }
                if (resp.statusCode >= 200 && resp.statusCode < 300) {
                    if (respBody && respBody.ok === true) {
                        return { ok: true, status: resp.statusCode }
                    }
                    var why = (respBody && respBody.error) ? respBody.error : "relay_declined"
                    console.log("[push] " + pushType + " skip: " + why)
                    return { ok: false, reason: why }
                }
                console.log("[push] " + pushType + " skip: relay_http_" + resp.statusCode)
                return { ok: false, status: resp.statusCode, reason: "relay_http" }
            } catch (httpErr) {
                console.log("[push] " + pushType + " skip: relay_timeout")
                return { ok: false, reason: "relay_timeout" }
            }
        }
        return { pbRecordId: pbRecordId, pushUserDisplayName: pushUserDisplayName, pushPreviewBody: pushPreviewBody, pushSendFcm: pushSendFcm }
    })()
    try {
        var record = e.record
        if (record.getString("invite_mode") !== "direct") return
        if (record.getString("status") !== "waiting") return

        var hostId = record.getString("host")
        var guestId = record.getString("guest")
        var roomId = kit.pbRecordId(record)
        if (!hostId || !guestId || !roomId) return

        var host = $app.findRecordById("users", hostId)
        var hostName = kit.pushUserDisplayName(host, hostId)
        var guest = $app.findRecordById("users", guestId)
        var fcmToken = guest ? guest.getString("fcm_token") : ""
        if (!fcmToken) {
            console.log("[push] game_invite skip: no fcm_token guest=" + guestId)
            return
        }
        var gameType = record.getString("game_type") || "game"
        var result = kit.pushSendFcm(
            fcmToken,
            hostName + " 邀请你一起玩",
            gameType + " · 点击接受",
            {
                type: "game_invite",
                room_id: roomId,
                room_code: record.getString("room_code") || "",
                game_type: gameType,
                host_pb_id: hostId,
                deep_link: "social_game_lobby/" + roomId,
            },
        )
        if (result.ok) {
            console.log("[push] game_invite sent room=" + roomId)
        }
    } catch (err) {
        console.log("[push] game_invite unexpected: " + err)
    } finally {
        e.next()
    }
}, "game_rooms")
