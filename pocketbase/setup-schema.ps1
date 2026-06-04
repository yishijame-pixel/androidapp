# FunLife PocketBase Phase 1 — 自动配置 users / friendships 集合与 API Rules
# 用法：先启动 PocketBase，再运行 .\setup-schema.ps1
param(
    [string]$BaseUrl = "http://127.0.0.1:8090",
    [string]$AdminEmail = "admin@funlife.local",
    [string]$AdminPassword = "FunLifePB2026!"
)

$ErrorActionPreference = "Stop"

function Get-AdminToken {
    $body = @{ identity = $AdminEmail; password = $AdminPassword } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/collections/_superusers/auth-with-password" `
        -Method POST -Body $body -ContentType "application/json"
    return $r.token
}

function Invoke-PbApi {
    param([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null)
    $headers = @{ Authorization = "Bearer $Token" }
    $params = @{
        Uri         = "$BaseUrl$Path"
        Method      = $Method
        Headers     = $headers
        ContentType = "application/json"
    }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 20 -Compress) }
    return Invoke-RestMethod @params
}

Write-Host ">> Login admin..."
$token = Get-AdminToken

Write-Host ">> Patch users collection..."
$users = Invoke-PbApi GET "/api/collections/users" $token
$existingNames = @($users.fields | ForEach-Object { $_.name })

$newFields = @()
if ("funlife_local_id" -notin $existingNames) {
    $newFields += @{
        hidden = $false; id = "number_funlife_local_id"; max = $null; min = $null
        name = "funlife_local_id"; onlyInt = $true; presentable = $false
        required = $true; system = $false; type = "number"
    }
}
if ("funlife_username" -notin $existingNames) {
    $newFields += @{
        autogeneratePattern = ""; hidden = $false; id = "text_funlife_username"
        max = 64; min = 2; name = "funlife_username"; pattern = ""
        presentable = $true; primaryKey = $false; required = $true
        system = $false; type = "text"
    }
}
if ("online" -notin $existingNames) {
    $newFields += @{
        hidden = $false; id = "bool_online"; name = "online"
        presentable = $false; required = $false; system = $false; type = "bool"
    }
}
if ("fcm_token" -notin $existingNames) {
    $newFields += @{
        autogeneratePattern = ""; hidden = $false; id = "text_fcm_token"
        max = 512; min = 0; name = "fcm_token"; pattern = ""
        presentable = $false; primaryKey = $false; required = $false
        system = $false; type = "text"
    }
}

$usersPatch = @{
    listRule   = '@request.auth.id != ""'
    viewRule   = '@request.auth.id != ""'
    createRule = ''
    updateRule = 'id = @request.auth.id'
    deleteRule = 'id = @request.auth.id'
    indexes    = @(
        'CREATE UNIQUE INDEX `idx_tokenKey__pb_users_auth_` ON `users` (`tokenKey`)',
        'CREATE UNIQUE INDEX `idx_email__pb_users_auth_` ON `users` (`email`) WHERE `email` != ''''',
        'CREATE UNIQUE INDEX `idx_funlife_username` ON `users` (`funlife_username`)'
    )
}
if ($newFields.Count -gt 0) {
    $usersPatch.fields = @($users.fields) + @($newFields)
    $names = ($newFields | ForEach-Object { $_.name }) -join ', '
    Write-Host "   add fields: $names"
} else {
    Write-Host "   users fields exist, sync rules/indexes only"
}

Invoke-PbApi PATCH "/api/collections/users" $token $usersPatch | Out-Null
Write-Host "   users OK"

Write-Host ">> friendships collection..."
$collections = Invoke-PbApi GET "/api/collections" $token
$hasFriendships = @($collections.items | Where-Object { $_.name -eq "friendships" }).Count -gt 0

$fbRules = @{
    listRule   = '@request.auth.id = requester || @request.auth.id = addressee'
    viewRule   = '@request.auth.id = requester || @request.auth.id = addressee'
    createRule = '@request.auth.id = requester'
    updateRule = '@request.auth.id = requester || @request.auth.id = addressee'
    deleteRule = '@request.auth.id = requester || @request.auth.id = addressee'
    indexes    = @(
        'CREATE UNIQUE INDEX `idx_friend_pair` ON `friendships` (`requester`, `addressee`)'
    )
}

if (-not $hasFriendships) {
    $friendshipsBody = @{
        name       = "friendships"
        type       = "base"
        listRule   = $fbRules.listRule
        viewRule   = $fbRules.viewRule
        createRule = $fbRules.createRule
        updateRule = $fbRules.updateRule
        deleteRule = $fbRules.deleteRule
        fields     = @(
            @{
                autogeneratePattern = "[a-z0-9]{15}"; hidden = $false; id = "text3208210256"
                max = 15; min = 15; name = "id"; pattern = "^[a-z0-9]+$"; presentable = $false
                primaryKey = $true; required = $true; system = $true; type = "text"
            },
            @{
                cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
                id = "relation_requester"; maxSelect = 1; minSelect = 0; name = "requester"
                presentable = $false; required = $true; system = $false; type = "relation"
            },
            @{
                cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
                id = "relation_addressee"; maxSelect = 1; minSelect = 0; name = "addressee"
                presentable = $false; required = $true; system = $false; type = "relation"
            },
            @{
                hidden = $false; id = "select_status"; maxSelect = 1; name = "status"
                presentable = $false; required = $true; system = $false; type = "select"
                values = @("pending", "accepted", "blocked")
            },
            @{
                hidden = $false; id = "autodate2990389176"; name = "created"
                onCreate = $true; onUpdate = $false; presentable = $false; system = $false; type = "autodate"
            },
            @{
                hidden = $false; id = "autodate3332085495"; name = "updated"
                onCreate = $true; onUpdate = $true; presentable = $false; system = $false; type = "autodate"
            }
        )
        indexes = $fbRules.indexes
    }
    Invoke-PbApi POST "/api/collections" $token $friendshipsBody | Out-Null
    Write-Host "   friendships created"
} else {
    Invoke-PbApi PATCH "/api/collections/friendships" $token $fbRules | Out-Null
    Write-Host "   friendships exists, rules synced"
}

Write-Host ">> conversations collection..."
$hasConversations = @($collections.items | Where-Object { $_.name -eq "conversations" }).Count -gt 0

$convRules = @{
    listRule   = '@request.auth.id = member_a || @request.auth.id = member_b'
    viewRule   = '@request.auth.id = member_a || @request.auth.id = member_b'
    createRule = '@request.auth.id = member_a || @request.auth.id = member_b'
    updateRule = '@request.auth.id = member_a || @request.auth.id = member_b'
    deleteRule = '@request.auth.id = member_a || @request.auth.id = member_b'
    indexes    = @(
        'CREATE UNIQUE INDEX `idx_conv_pair_key` ON `conversations` (`pair_key`)'
    )
}

$convFields = @(
    @{
        autogeneratePattern = "[a-z0-9]{15}"; hidden = $false; id = "text3208210256"
        max = 15; min = 15; name = "id"; pattern = "^[a-z0-9]+$"; presentable = $false
        primaryKey = $true; required = $true; system = $true; type = "text"
    },
    @{
        cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
        id = "relation_member_a"; maxSelect = 1; minSelect = 0; name = "member_a"
        presentable = $false; required = $true; system = $false; type = "relation"
    },
    @{
        cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
        id = "relation_member_b"; maxSelect = 1; minSelect = 0; name = "member_b"
        presentable = $false; required = $true; system = $false; type = "relation"
    },
    @{
        autogeneratePattern = ""; hidden = $false; id = "text_pair_key"
        max = 64; min = 3; name = "pair_key"; pattern = ""
        presentable = $false; primaryKey = $false; required = $true
        system = $false; type = "text"
    },
    @{
        autogeneratePattern = ""; hidden = $false; id = "text_last_preview"
        max = 200; min = 0; name = "last_preview"; pattern = ""
        presentable = $false; primaryKey = $false; required = $false
        system = $false; type = "text"
    },
    @{
        hidden = $false; id = "number_last_message_at"; max = $null; min = $null
        name = "last_message_at"; onlyInt = $false; presentable = $false
        required = $false; system = $false; type = "number"
    },
    @{
        hidden = $false; id = "autodate2990389176"; name = "created"
        onCreate = $true; onUpdate = $false; presentable = $false; system = $false; type = "autodate"
    },
    @{
        hidden = $false; id = "autodate3332085495"; name = "updated"
        onCreate = $true; onUpdate = $true; presentable = $false; system = $false; type = "autodate"
    }
)

if (-not $hasConversations) {
    $convBody = @{
        name       = "conversations"
        type       = "base"
        listRule   = $convRules.listRule
        viewRule   = $convRules.viewRule
        createRule = $convRules.createRule
        updateRule = $convRules.updateRule
        deleteRule = $convRules.deleteRule
        fields     = $convFields
        indexes    = $convRules.indexes
    }
    Invoke-PbApi POST "/api/collections" $token $convBody | Out-Null
    Write-Host "   conversations created"
} else {
    Invoke-PbApi PATCH "/api/collections/conversations" $token $convRules | Out-Null
    Write-Host "   conversations exists, rules synced"
}

$collections = Invoke-PbApi GET "/api/collections" $token
$convMeta = @($collections.items | Where-Object { $_.name -eq "conversations" })[0]
$convCollectionId = $convMeta.id

Write-Host ">> messages collection..."
$hasMessages = @($collections.items | Where-Object { $_.name -eq "messages" }).Count -gt 0

$msgRules = @{
    listRule   = '@request.auth.id = member_a || @request.auth.id = member_b'
    viewRule   = '@request.auth.id = member_a || @request.auth.id = member_b'
    createRule = '@request.auth.id = sender && (@request.auth.id = member_a || @request.auth.id = member_b)'
    updateRule = ''
    deleteRule = ''
    indexes    = @(
        'CREATE INDEX `idx_msg_conversation_created` ON `messages` (`conversation`, `created`)'
    )
}

$msgFields = @(
    @{
        autogeneratePattern = "[a-z0-9]{15}"; hidden = $false; id = "text3208210256"
        max = 15; min = 15; name = "id"; pattern = "^[a-z0-9]+$"; presentable = $false
        primaryKey = $true; required = $true; system = $true; type = "text"
    },
    @{
        cascadeDelete = $true; collectionId = $convCollectionId; hidden = $false
        id = "relation_conversation"; maxSelect = 1; minSelect = 0; name = "conversation"
        presentable = $false; required = $true; system = $false; type = "relation"
    },
    @{
        cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
        id = "relation_member_a"; maxSelect = 1; minSelect = 0; name = "member_a"
        presentable = $false; required = $true; system = $false; type = "relation"
    },
    @{
        cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
        id = "relation_member_b"; maxSelect = 1; minSelect = 0; name = "member_b"
        presentable = $false; required = $true; system = $false; type = "relation"
    },
    @{
        cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
        id = "relation_sender"; maxSelect = 1; minSelect = 0; name = "sender"
        presentable = $false; required = $true; system = $false; type = "relation"
    },
    @{
        autogeneratePattern = ""; hidden = $false; id = "text_body"
        max = 2000; min = 1; name = "body"; pattern = ""
        presentable = $true; primaryKey = $false; required = $true
        system = $false; type = "text"
    },
    @{
        hidden = $false; id = "autodate2990389176"; name = "created"
        onCreate = $true; onUpdate = $false; presentable = $false; system = $false; type = "autodate"
    }
)

if (-not $hasMessages) {
    $msgBody = @{
        name       = "messages"
        type       = "base"
        listRule   = $msgRules.listRule
        viewRule   = $msgRules.viewRule
        createRule = $msgRules.createRule
        updateRule = $msgRules.updateRule
        deleteRule = $msgRules.deleteRule
        fields     = $msgFields
        indexes    = $msgRules.indexes
    }
    Invoke-PbApi POST "/api/collections" $token $msgBody | Out-Null
    Write-Host "   messages created"
} else {
    $existingMsg = Invoke-PbApi GET "/api/collections/messages" $token
    $existingMsgNames = @($existingMsg.fields | ForEach-Object { $_.name })
    $msgPatch = @{
        listRule   = $msgRules.listRule
        viewRule   = $msgRules.viewRule
        createRule = $msgRules.createRule
        updateRule = $msgRules.updateRule
        deleteRule = $msgRules.deleteRule
        indexes    = $msgRules.indexes
    }
    $newMsgFields = @()
    if ("member_a" -notin $existingMsgNames) {
        $newMsgFields += @{
            cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
            id = "relation_member_a"; maxSelect = 1; minSelect = 0; name = "member_a"
            presentable = $false; required = $true; system = $false; type = "relation"
        }
    }
    if ("member_b" -notin $existingMsgNames) {
        $newMsgFields += @{
            cascadeDelete = $false; collectionId = "_pb_users_auth_"; hidden = $false
            id = "relation_member_b"; maxSelect = 1; minSelect = 0; name = "member_b"
            presentable = $false; required = $true; system = $false; type = "relation"
        }
    }
    if ($newMsgFields.Count -gt 0) {
        $msgPatch.fields = @($existingMsg.fields) + @($newMsgFields)
        Write-Host "   messages add fields: $(($newMsgFields | ForEach-Object { $_.name }) -join ', ')"
    }
    Invoke-PbApi PATCH "/api/collections/messages" $token $msgPatch | Out-Null
    Write-Host "   messages exists, rules synced"
}

Write-Host ""
Write-Host "=== Phase 1 + Phase 2 schema ready ==="
Write-Host "Dashboard: $BaseUrl/_/"
Write-Host "users: funlife_local_id, funlife_username (unique), online"
Write-Host "friendships: requester, addressee, status"
Write-Host "conversations: member_a, member_b, pair_key (unique), last_preview, last_message_at"
Write-Host "messages: conversation, sender, body"
