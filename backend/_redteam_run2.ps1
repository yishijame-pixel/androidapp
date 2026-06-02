$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$SECRET = "0f2eed661ccce58ce69d6704094f283c7e2d4ad14285a1bd1f18986ad64aaada93564581a4dcaea2ef12a9a9502d70b0"

function Inv($fn, $obj) {
    $j = ($obj | ConvertTo-Json -Compress -Depth 10).Replace('"','\"')
    $out = cmd /c "tcb fn invoke $fn --params `"$j`" 2>&1"
    $line = ($out | Select-String "Return result").Line
    if ($line) {
        $idx = $line.IndexOf('{')
        if ($idx -ge 0) { return $line.Substring($idx) }
    }
    return "(no result)"
}
function PJ($s) { try { return $s | ConvertFrom-Json } catch { return $null } }

function B64Url($bytes) {
    return [Convert]::ToBase64String($bytes).Replace('+','-').Replace('/','_').TrimEnd('=')
}
function HmacBytes($secret, $msg) {
    $h = New-Object System.Security.Cryptography.HMACSHA256
    $h.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
    return $h.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($msg))
}
function HmacHex($secret, $msg) {
    $b = HmacBytes $secret $msg
    return ($b | ForEach-Object { $_.ToString("x2") }) -join ""
}
function IssueToken($username, $deviceId, $issuedAt) {
    $payload = (@{u=$username; d=$deviceId; t=$issuedAt} | ConvertTo-Json -Compress)
    $p64 = B64Url ([System.Text.Encoding]::UTF8.GetBytes($payload))
    $sigB = HmacBytes $SECRET $p64
    return "v1." + $p64 + "." + (B64Url $sigB)
}
function CanonicalJson($hash) {
    $sorted = [ordered]@{}
    $hash.Keys | Sort-Object | ForEach-Object { $sorted[$_] = $hash[$_] }
    return ($sorted | ConvertTo-Json -Compress)
}

$DEV32 = "deviceA_aaaaaaaaaaaaaaaaaaaaaaaa"
$DEV_B = "deviceB_bbbbbbbbbbbbbbbbbbbbbbbb"
$USER1 = "redA_user1"
$USER2 = "redA_user2"
$NOW   = [int][DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$NOWMS = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Host "DEV32 len=$($DEV32.Length) NOW=$NOW"

# Register a real test account to get a real token (signed by the actual server secret)
$pwProof = (1..64 | ForEach-Object { 'a' }) -join ''  # 64-char fake hash
$regResp = Inv "register_log" @{username=$USER1; nickname="rt"; deviceId=$DEV32; passwordProof=$pwProof}
Write-Host "register_log: $regResp"
$tok1 = (PJ $regResp).deviceToken
if (-not $tok1) {
    Write-Host "[FATAL] cannot get real token from register_log; abort A group"
    return
}
Write-Host "got real tok1 length=$($tok1.Length)"

Write-Host ""
Write-Host "==== A. coin_log ===="
$base = @{username=$USER1; deviceId=$DEV32; deviceToken=$tok1; op="earn"; amount=1; reason="rt"; balance=10; totalEarned=10; totalSpent=0; ts=$NOWMS}

$nonce = "nonce_rt_" + (Get-Random -Maximum 999999)
$b1 = $base.Clone(); $b1.nonce = $nonce
Write-Host "[A1] 1st coin_log -> ok:true"
Inv "coin_log" $b1

Write-Host "[A2] 2nd same nonce -> REPLAY"
Inv "coin_log" $b1

$b2 = $base.Clone(); $b2.nonce = "nonce_rt_skew_" + (Get-Random); $b2.ts = ($NOW - 1800) * 1000
Write-Host "[A3] ts -30min -> TS_OUT_OF_WINDOW"
Inv "coin_log" $b2

$b3 = $base.Clone(); $b3.username = $USER2; $b3.nonce = "nonce_rt_cross_" + (Get-Random)
Write-Host "[A4] user2 with user1 token -> AUTH_USER_MISMATCH"
Inv "coin_log" $b3

$b4 = $base.Clone(); $b4.deviceId = $DEV_B; $b4.nonce = "nonce_rt_dev_" + (Get-Random)
Write-Host "[A5] switch device keep token -> AUTH_DEVICE_MISMATCH"
Inv "coin_log" $b4

$b5 = $base.Clone(); $b5.Remove("deviceToken"); $b5.nonce = "nonce_rt_nt_" + (Get-Random)
Write-Host "[A6] no token -> AUTH_REQUIRED"
Inv "coin_log" $b5

$b6 = $base.Clone(); $b6.deviceToken = "v1.fakefake.fakesig"; $b6.nonce = "nonce_rt_fk_" + (Get-Random)
Write-Host "[A7] fake token -> AUTH_INVALID"
Inv "coin_log" $b6

Write-Host ""
Write-Host "==== B. verify ===="
$VIP = "REDVIPRT" + (Get-Random -Maximum 999999)
Inv "redteam" @{action="seed_vip"; code=$VIP; skuCode="VIP_NORMAL"} | Out-Null
$rd = Inv "redeem" @{code=$VIP; deviceId=$DEV32; userId=9001}
$rdObj = PJ $rd
$cert = $rdObj.certificate
$sig  = $rdObj.signature
if ($cert -eq $null) {
    Write-Host "[B0] redeem FAIL: $rd"
    return
}
Write-Host "[B0] redeem ok skuCode=$($cert.skuCode) issuedAt=$($cert.issuedAt)"

Write-Host "[B1] valid cert -> ok:true"
Inv "verify" @{certificate=$cert; signature=$sig}

Write-Host "[B2] tampered vipLevel -> BAD_SIGNATURE"
$tampered = @{
    deviceId=$cert.deviceId; skuCode=$cert.skuCode; vipLevel=99;
    expireDate=$cert.expireDate; bonusCoins=$cert.bonusCoins;
    issuedAt=$cert.issuedAt; exp=$cert.exp
}
Inv "verify" @{certificate=$tampered; signature=$sig}

$expCert = @{
    deviceId=$cert.deviceId; skuCode=$cert.skuCode; vipLevel=$cert.vipLevel;
    expireDate=$cert.expireDate; bonusCoins=0;
    issuedAt=($NOW-86400); exp=($NOW-3600)
}
$expCanon = CanonicalJson $expCert
$expSig = HmacHex $SECRET $expCanon
Write-Host "[B3] cert.exp lt now -> CERT_EXPIRED"
Inv "verify" @{certificate=$expCert; signature=$expSig}

Write-Host "[B4] device-level revocation -> REVOKED"
$revOut = Inv "redteam" @{action="revoke"; deviceId=$DEV32; revokedAt=([int64]$cert.issuedAt + 3600); reason="rt"}
Write-Host "  revoke: $revOut"
$inspOut = Inv "redteam" @{action="inspect_rev"; deviceId=$DEV32}
Write-Host "  inspect: $inspOut"
Inv "verify" @{certificate=$cert; signature=$sig}

Write-Host "[B5] unrevoke -> ok:true again"
Inv "redteam" @{action="unrevoke"; deviceId=$DEV32} | Out-Null
Inv "verify" @{certificate=$cert; signature=$sig}

Write-Host ""
Write-Host "==== C. migrate ===="
Write-Host "[C1] migrate no token -> AUTH_REQUIRED"
Inv "migrate" @{code=$VIP; newDeviceId=$DEV_B; oldDeviceId=$DEV32; username=$USER1}

Write-Host "[C2] migrate fake token -> AUTH_INVALID"
Inv "migrate" @{code=$VIP; newDeviceId=$DEV_B; oldDeviceId=$DEV32; username=$USER1; deviceToken="v1.xxx.yyy"}

$tokB = IssueToken $USER1 $DEV_B $NOW
Write-Host "[C3] migrate no oldDeviceId -> OLD_DEVICE_REQUIRED"
Inv "migrate" @{code=$VIP; newDeviceId=$DEV_B; username=$USER1; deviceToken=$tokB}

Write-Host "[C4] migrate wrong oldDeviceId -> OLD_DEVICE_MISMATCH"
Inv "migrate" @{code=$VIP; newDeviceId=$DEV_B; oldDeviceId="wrong_old_xxxxxxxxxxxxxxxxxxxx"; username=$USER1; deviceToken=$tokB}

Write-Host ""
Write-Host "==== CLEANUP ===="
Inv "redteam" @{action="cleanup_code"; code=$VIP} | Out-Null
Write-Host "DONE"
