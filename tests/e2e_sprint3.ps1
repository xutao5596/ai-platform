# Sprint 3 E2E 鍏ㄩ噺娴嬭瘯
# 鐢ㄦ硶: powershell -ExecutionPolicy Bypass -File tests\e2e_sprint3.ps1

$base = "http://localhost:8080"
$pass = 0
$fail = 0
$results = New-Object System.Collections.ArrayList

function Record($name, $status, $detail = "") {
    if ($status -eq "ok") { $script:pass++; Write-Host "  PASS: $name" -ForegroundColor Green }
    else { $script:fail++; Write-Host "  FAIL: $name - $status (detail: $detail)" -ForegroundColor Red }
    [void]$script:results.Add(@{ name = $name; status = $status; detail = $detail })
}

function CkReq($r, $expectedCode = 200) {
    if ($null -eq $r) { return @{ ok = $false; msg = "response null" } }
    if ($r.code -ne $expectedCode) { return @{ ok = $false; msg = "code=$($r.code) msg=$($r.message)" } }
    return @{ ok = $true; data = $r.data }
}

# Login
$login = Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$tok = $login.data.accessToken
$global:h = @{ Authorization = "Bearer $tok" }
Write-Host "`nLogin OK, token len: $($tok.Length)"

# Helper: write body to temp file (avoids PS JSON string concat issues)
function Post-Json($url, $body, $hdr) {
    $body | Out-File "D:\Projet\AI-Platform\tests\_req.json" -Encoding utf8
    return Invoke-RestMethod -Uri $url -Method Post -ContentType "application/json; charset=utf-8" -Headers $hdr -InFile "D:\Projet\AI-Platform\tests\_req.json"
}

function Put-Json($url, $body, $hdr) {
    $body | Out-File "D:\Projet\AI-Platform\tests\_req.json" -Encoding utf8
    return Invoke-RestMethod -Uri $url -Method Put -ContentType "application/json; charset=utf-8" -Headers $hdr -InFile "D:\Projet\AI-Platform\tests\_req.json"
}

# Build flow create body: nested JSON, design escaped properly
function Flow-Body($name, $designJson) {
    # Use hashtable + ConvertTo-Json to get proper escaping
    $obj = @{ projectId = 1; name = $name; design = $designJson }
    return $obj | ConvertTo-Json -Depth 10 -Compress
}

# === Section 1: 8 Nodes ===
Write-Host "`n=== Section 1: 8 Nodes Execution ===" -ForegroundColor Cyan

# 1.1 Start鈫扙nd
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t11" $designJson
    Write-Host "    [1.1 body] $body" -ForegroundColor DarkGray
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.1 Start-End" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{"input":{"foo":"bar"}}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.1 Start-End" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.1 Start-End" "fail" "status=$($ck.data.status)" }
    else { Record "1.1 Start-End" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.1 Start-End" "fail" $_.ToString() }

# 1.2 SetVar
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"setv","type":"set_var","data":{"name":"greeting","value":"hello {{input.name}}"}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"setv"},{"source":"setv","target":"end"}]}'
    $body = Flow-Body "t12" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.2 SetVar" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{"input":{"name":"world"}}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.2 SetVar" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.2 SetVar" "fail" "status=$($ck.data.status) output=$($ck.data.output)" }
    else { Record "1.2 SetVar" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.2 SetVar" "fail" $_.ToString() }

# 1.3 IfElse true
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"if","type":"if_else","data":{"expression":"x > 10"}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"if"},{"source":"if","target":"end","data":{"result":true}}]}'
    $body = Flow-Body "t13" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.3 IfElse true" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{"input":{"x":15}}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.3 IfElse true" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.3 IfElse true" "fail" "status=$($ck.data.status)" }
    else { Record "1.3 IfElse true" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.3 IfElse true" "fail" $_.ToString() }

# 1.4 IfElse false
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"if","type":"if_else","data":{"expression":"x > 10"}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"if"},{"source":"if","target":"end","data":{"result":false}}]}'
    $body = Flow-Body "t14" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.4 IfElse false" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{"input":{"x":5}}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.4 IfElse false" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.4 IfElse false" "fail" "status=$($ck.data.status)" }
    else { Record "1.4 IfElse false" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.4 IfElse false" "fail" $_.ToString() }

# 1.5 HTTP GET
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"http","type":"http","data":{"url":"https://jsonplaceholder.typicode.com/posts/1","method":"GET"}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"http"},{"source":"http","target":"end"}]}'
    $body = Flow-Body "t15" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.5 HTTP GET" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.5 HTTP GET" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.5 HTTP GET" "fail" "status=$($ck.data.status) output=$($ck.data.output)" }
    else { Record "1.5 HTTP GET" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.5 HTTP GET" "fail" $_.ToString() }

# 1.6 HTTP POST
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"http","type":"http","data":{"url":"https://jsonplaceholder.typicode.com/posts","method":"POST","body":"{\"title\":\"e2e\"}","headers":{"Content-Type":"application/json"}}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"http"},{"source":"http","target":"end"}]}'
    $body = Flow-Body "t16" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.6 HTTP POST" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.6 HTTP POST" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.6 HTTP POST" "fail" "status=$($ck.data.status)" }
    else { Record "1.6 HTTP POST" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.6 HTTP POST" "fail" $_.ToString() }

# 1.7 LLM (no api key, should fail)
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"llm","type":"llm","data":{"modelId":1,"prompt":"Hello"}}],"edges":[{"source":"start","target":"llm"}]}'
    $body = Flow-Body "t17" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.7 LLM (no key)" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.7 LLM (no key)" "fail" $ck.msg }
    elseif ($ck.data.status -ne "failed") { Record "1.7 LLM (no key)" "fail" "expected failed, got $($ck.data.status) output=$($ck.data.output)" }
    elseif (-not $ck.data.errorMsg) { Record "1.7 LLM (no key)" "fail" "no errorMsg" }
    else { Record "1.7 LLM (no key)" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.7 LLM (no key)" "fail" $_.ToString() }

# 1.8 KB (no KB - empty project, returns empty results success)
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"ks","type":"knowledge_search","data":{"kbIds":"99999","query":"test","topK":3}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"ks"},{"source":"ks","target":"end"}]}'
    $body = Flow-Body "t18" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.8 KB (no KB)" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.8 KB (no KB)" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "1.8 KB (no KB)" "fail" "status=$($ck.data.status)" }
    else { Record "1.8 KB (no KB)" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.8 KB (no KB)" "fail" $_.ToString() }

# 1.9 Prompt (no prompt)
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"p","type":"prompt","data":{"promptCode":"nonexistent","vars":{}}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"p"},{"source":"p","target":"end"}]}'
    $body = Flow-Body "t19" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.9 Prompt (no code)" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.9 Prompt (no code)" "fail" $ck.msg }
    elseif ($ck.data.status -ne "failed") { Record "1.9 Prompt (no code)" "fail" "expected failed, got $($ck.data.status)" }
    else { Record "1.9 Prompt (no code)" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.9 Prompt (no code)" "fail" $_.ToString() }

# 1.10 Failure propagation
try {
    $designJson = '{"nodes":[{"id":"start","type":"start"},{"id":"http","type":"http","data":{"url":"http://localhost:99999/x","method":"GET"}},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"http"},{"source":"http","target":"end"}]}'
    $body = Flow-Body "t110" $designJson
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.10 Failure propagation" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "1.10 Failure propagation" "fail" $ck.msg }
    elseif ($ck.data.status -ne "failed") { Record "1.10 Failure propagation" "fail" "expected failed, got $($ck.data.status)" }
    elseif (-not $ck.data.errorMsg) { Record "1.10 Failure propagation" "fail" "no errorMsg" }
    else { Record "1.10 Failure propagation" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "1.10 Failure propagation" "fail" $_.ToString() }

# === Section 2: 5 Triggers ===
Write-Host "`n=== Section 2: 5 Triggers ===" -ForegroundColor Cyan

# 2.1 Manual
try {
    $design = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t21" $design
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.1 Manual trigger" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/triggers" '{"type":"manual","config":"{}","projectId":1}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.1 Manual trigger" "fail" $ck.msg; return }
    $tid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/run" '{}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.1 Manual trigger" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "2.1 Manual trigger" "fail" "status=$($ck.data.status)" }
    else { Record "2.1 Manual trigger" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers/$tid" -Method Delete -Headers $global:h | Out-Null
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "2.1 Manual trigger" "fail" $_.ToString() }

# 2.2 Webhook
try {
    $design = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t22" $design
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.2 Webhook trigger" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/triggers" '{"type":"webhook","config":"{}","projectId":1}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.2 Webhook trigger" "fail" $ck.msg; return }
    $tid = $ck.data
    $trigs = Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers" -Headers $global:h
    $token = ($trigs.data[0].config | ConvertFrom-Json).token
    if (-not $token) { Record "2.2 Webhook trigger" "fail" "no token in config"; return }
    $wbBody = '{"foo":"webhook"}'
    $wbBody | Out-File "D:\Projet\AI-Platform\tests\_wb.json" -Encoding utf8
    $r = Invoke-RestMethod -Uri "$base/api/v1/webhook/flow/$token" -Method Post -ContentType "application/json" -Body $wbBody -TimeoutSec 30
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.2 Webhook trigger" "fail" $ck.msg }
    elseif ($ck.data.status -ne "success") { Record "2.2 Webhook trigger" "fail" "status=$($ck.data.status) output=$($ck.data.output)" }
    else { Record "2.2 Webhook trigger" "ok" }
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers/$tid" -Method Delete -Headers $global:h | Out-Null
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "2.2 Webhook trigger" "fail" $_.ToString() }

# 2.3 Cron
try {
    $design = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t23" $design
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.3 Cron trigger" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/triggers" '{"type":"cron","config":"{\"cron\":\"0 0 0 * * ?\",\"input\":{}}","projectId":1}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.3 Cron trigger" "fail" $ck.msg; return }
    $tid = $ck.data
    $trigs = Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers" -Headers $global:h
    if ($trigs.data[0].type -ne "cron") { Record "2.3 Cron trigger" "fail" "type=$($trigs.data[0].type)"; return }
    Record "2.3 Cron trigger" "ok"
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers/$tid" -Method Delete -Headers $global:h | Out-Null
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "2.3 Cron trigger" "fail" $_.ToString() }

# 2.4 Event
try {
    $design = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t24" $design
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.4 Event trigger" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/triggers" '{"type":"event","config":"{\"eventType\":\"flow.run.success\"}","projectId":1}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.4 Event trigger" "fail" $ck.msg; return }
    $tid = $ck.data
    Record "2.4 Event trigger" "ok"
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers/$tid" -Method Delete -Headers $global:h | Out-Null
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "2.4 Event trigger" "fail" $_.ToString() }

# 2.5 Chained
try {
    $design = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t25" $design
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.5 Chained trigger" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/triggers" '{"type":"chained","config":"{}","projectId":1}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "2.5 Chained trigger" "fail" $ck.msg; return }
    $tid = $ck.data
    Record "2.5 Chained trigger" "ok"
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/triggers/$tid" -Method Delete -Headers $global:h | Out-Null
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "2.5 Chained trigger" "fail" $_.ToString() }

# === Section 3: Versions ===
Write-Host "`n=== Section 3: Flow Versions ===" -ForegroundColor Cyan

try {
    $design = '{"nodes":[{"id":"start","type":"start"},{"id":"end","type":"end"}],"edges":[{"source":"start","target":"end"}]}'
    $body = Flow-Body "t31" $design
    $r = Post-Json "$base/api/v1/flow" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "3.1 Versions CRUD" "fail" $ck.msg; return }
    $fid = $ck.data
    $r = Post-Json "$base/api/v1/flow/$fid/versions" '{"design":"{\"nodes\":[{\"id\":\"v1\",\"type\":\"start\"}]}","chain":"","changelog":"v1"}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "3.1 Versions CRUD" "fail" "create: $($ck.msg)"; return }
    $vid = $ck.data
    $v = Invoke-RestMethod -Uri "$base/api/v1/flow/$fid/versions" -Headers $global:h
    if ($v.data.Count -ne 1) { Record "3.1 Versions CRUD" "fail" "count=$($v.data.Count)"; return }
    if ($v.data[0].version -ne 1) { Record "3.1 Versions CRUD" "fail" "version=$($v.data[0].version)"; return }
    Record "3.1 Versions CRUD" "ok"
    Invoke-RestMethod -Uri "$base/api/v1/flow/$fid" -Method Delete -Headers $global:h | Out-Null
} catch { Record "3.1 Versions CRUD" "fail" $_.ToString() }

# === Section 4: Custom Nodes ===
Write-Host "`n=== Section 4: Custom Nodes ===" -ForegroundColor Cyan

try {
    $cnTypeKey = "custom_e2e_$([guid]::NewGuid().ToString('N').Substring(0,8))"
    $body = '{"projectId":1,"name":"E2E Custom","typeKey":"' + $cnTypeKey + '","category":"tool","configSchema":"{}","implementation":"console.log","status":1}'
    $r = Post-Json "$base/api/v1/flow/custom-nodes" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "4.1 Custom Node CRUD" "fail" $ck.msg; return }
    $cnid = $ck.data
    $g = Invoke-RestMethod -Uri "$base/api/v1/flow/custom-nodes/$cnid" -Headers $global:h
    if ($g.data.typeKey -ne $cnTypeKey) { Record "4.1 Custom Node CRUD" "fail" "typeKey mismatch: expected=$cnTypeKey got=$($g.data.typeKey)"; return }
    Invoke-RestMethod -Uri "$base/api/v1/flow/custom-nodes/$cnid" -Method Delete -Headers $global:h | Out-Null
    Record "4.1 Custom Node CRUD" "ok"
} catch { Record "4.1 Custom Node CRUD" "fail" $_.ToString() }

# === Section 5: 鍔╂墜 + 宸ュ叿 ===
Write-Host "`n=== Section 5: Assistant + Tools ===" -ForegroundColor Cyan

# 5.1 鍔╂墜 CRUD
try {
    $body = '{"projectId":1,"name":"E2E Bot","persona":"You are helpful","modelId":1,"toolsEnabled":"[\"calculator\"]"}'
    $r = Post-Json "$base/api/v1/assistant" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "5.1 Assistant CRUD" "fail" $ck.msg; return }
    $aid = $ck.data
    $g = Invoke-RestMethod -Uri "$base/api/v1/assistant/$aid" -Headers $global:h
    if ($g.data.name -ne "E2E Bot") { Record "5.1 Assistant CRUD" "fail" "name mismatch"; return }
    Invoke-RestMethod -Uri "$base/api/v1/assistant/$aid" -Method Delete -Headers $global:h | Out-Null
    Record "5.1 Assistant CRUD" "ok"
} catch { Record "5.1 Assistant CRUD" "fail" $_.ToString() }

# 5.2 宸ュ叿鍒楄〃
try {
    $r = Invoke-RestMethod -Uri "$base/api/v1/assistant/tools" -Headers $global:h
    if ($r.data.Count -ne 8) { Record "5.2 Tools list (8)" "fail" "count=$($r.data.Count)"; return }
    Record "5.2 Tools list (8)" "ok"
} catch { Record "5.2 Tools list (8)" "fail" $_.ToString() }

# 5.3 宸ュ叿娴嬭瘯
$tools = @(
    @{ name = "5.3a Calculator"; body = '{"toolName":"calculator","args":{"expression":"100/4"}}'; expect = "25" }
    @{ name = "5.3b Current time"; body = '{"toolName":"current_time","args":{}}'; expect = $null }
    @{ name = "5.3c List projects"; body = '{"toolName":"list_projects","args":{}}'; expect = $null }
    @{ name = "5.3d HTTP tool"; body = '{"toolName":"http_request","args":{"url":"https://jsonplaceholder.typicode.com/posts/1","method":"GET"}}'; expect = $null }
    @{ name = "5.3e Subflow (no flow)"; body = '{"toolName":"run_flow","args":{"flowId":99999}}'; expect = "fail" }
    @{ name = "5.3f KB search (no KB)"; body = '{"toolName":"search_kb","args":{"kbIds":"99999","query":"x"}}'; expect = "fail" }
    @{ name = "5.3g Project members"; body = '{"toolName":"project_members","args":{"projectId":1}}'; expect = $null }
    @{ name = "5.3h Code run"; body = '{"toolName":"code_run","args":{"code":"1+2*3"}}'; expect = $null }
)
foreach ($t in $tools) {
    try {
        $r = Post-Json "$base/api/v1/assistant/tools/test" $t.body $global:h
        $ck = CkReq $r
        if (-not $ck.ok) { Record $t.name "fail" $ck.msg; continue }
        if ($t.expect -eq "fail") {
            if ($ck.data.success) { Record $t.name "fail" "expected fail but got success: $($ck.data.output)"; continue }
        } else {
            if (-not $ck.data.output -and -not $ck.data.result) { Record $t.name "fail" "empty output: $($ck.data | ConvertTo-Json)"; continue }
            if ($t.expect -and ($ck.data.output -notmatch $t.expect) -and ($ck.data.result -notmatch $t.expect)) {
                Record $t.name "fail" "expected '$($t.expect)' in output, got '$($ck.data.output)' / '$($ck.data.result)'"; continue
            }
        }
        Record $t.name "ok"
    } catch { Record $t.name "fail" $_.ToString() }
}

# 5.4 浜嬩欢璁㈤槄
try {
    $body = '{"projectId":1,"name":"Sub E2E","persona":"x","modelId":1}'
    $r = Post-Json "$base/api/v1/assistant" $body $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "5.4 EventSub" "fail" $ck.msg; return }
    $aid = $ck.data
    $r = Post-Json "$base/api/v1/assistant/$aid/events" '{"eventType":"flow.run.failed","enabled":1}' $global:h
    $ck = CkReq $r
    if (-not $ck.ok) { Record "5.4 EventSub" "fail" $ck.msg; return }
    $eid = $ck.data
    $l = Invoke-RestMethod -Uri "$base/api/v1/assistant/$aid/events" -Headers $global:h
    if ($l.data.Count -ne 1) { Record "5.4 EventSub" "fail" "count=$($l.data.Count)"; return }
    Invoke-RestMethod -Uri "$base/api/v1/assistant/$aid/events/$eid" -Method Delete -Headers $global:h | Out-Null
    Invoke-RestMethod -Uri "$base/api/v1/assistant/$aid" -Method Delete -Headers $global:h | Out-Null
    Record "5.4 EventSub" "ok"
} catch { Record "5.4 EventSub" "fail" $_.ToString() }

# === Summary ===
Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "PASS: $pass" -ForegroundColor Green
Write-Host "FAIL: $fail" -ForegroundColor Red
Write-Host "TOTAL: $($pass + $fail)"
Write-Host ""
if ($fail -gt 0) {
    Write-Host "Failed cases:" -ForegroundColor Red
    $results | Where-Object { $_.status -ne "ok" } | ForEach-Object { Write-Host "  - $($_.name): $($_.detail)" -ForegroundColor Yellow }
}

# Persist
$results | Export-Csv "D:\Projet\AI-Platform\tests\e2e_results.csv" -NoTypeInformation -Encoding utf8
exit $fail











