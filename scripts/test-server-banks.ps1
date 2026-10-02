param([switch]$PrepareOnly, [string]$ReviewDirectory = 'build/review/server-banks-2026-10-01')
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$out = Join-Path $root 'build/algocraft-bank-multiplayer'
$review = [IO.Path]::GetFullPath((Join-Path $root $ReviewDirectory))
$runtimes = @('run-bank-server','run-bank-client-a','run-bank-client-b')
$tasks = @('runBankServer','runBankClientA','runBankClientB')
$saved = @{}
$children = @()
$priorJava = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Select-Object -ExpandProperty ProcessId)
Push-Location $root
try {
    New-Item -ItemType Directory -Force $out,$review | Out-Null
    if ($PrepareOnly) {
        & ./mod-build.ps1 gradle prepareBankServerRun prepareBankClientARun prepareBankClientBRun --no-daemon --console=plain *> (Join-Path $review 'topology-prepare.log')
        exit $LASTEXITCODE
    }
    # All three processes belong to this declared topology, with three distinct resource leases.
    $sourceHashes = @{}
    foreach ($file in (Get-ChildItem (Join-Path $root 'src/main') -Recurse -File)) {
        $sourceHashes[[IO.Path]::GetRelativePath($root,$file.FullName)] = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    [ordered]@{processStartCount=3;worldLoadCount=3;serverPort=25586;clientWebPorts=@(3011,3012);runtimes=$runtimes;sourceHashes=$sourceHashes} |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $out 'topology.json') -Encoding utf8NoBOM
    foreach ($name in @('server-ready.json','server-result.json','A-first.json','B-first.json','A-result.json','B-result.json')) {
        $path = Join-Path $out $name
        if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path }
    }
    foreach ($runtime in $runtimes) {
        foreach ($relative in @('options.txt','config/fml.toml','config/algocraft-common.toml','server.properties','eula.txt')) {
            $path = Join-Path (Join-Path $root $runtime) $relative
            $saved[$path] = if (Test-Path -LiteralPath $path) {[IO.File]::ReadAllBytes($path)} else {$null}
        }
    }
    $before = foreach ($path in $saved.Keys) {
        [ordered]@{path=$path;existed=$null -ne $saved[$path];sha256=if($null -ne $saved[$path]){[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($saved[$path])).ToLowerInvariant()}else{$null}}
    }
    $before | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $out 'configuration-before.json') -Encoding utf8NoBOM
    $serverRuntime = Join-Path $root $runtimes[0]
    New-Item -ItemType Directory -Force $serverRuntime | Out-Null
    @('server-ip=127.0.0.1','server-port=25586','online-mode=false','enforce-secure-profile=false','max-players=2','view-distance=2','simulation-distance=2','spawn-protection=0','difficulty=peaceful','level-type=minecraft:flat',('level-name=bank-fixture-'+[Guid]::NewGuid().ToString('N')),'generate-structures=false','sync-chunk-writes=false') |
        Set-Content -LiteralPath (Join-Path $serverRuntime 'server.properties') -Encoding utf8NoBOM
    'eula=true' | Set-Content -LiteralPath (Join-Path $serverRuntime 'eula.txt') -Encoding utf8NoBOM
    $pwsh = (Get-Command pwsh).Source
    $worker = Join-Path $PSScriptRoot 'test-server-banks-worker.ps1'
    for ($i=0;$i -lt 3;$i++) {
        $arguments = @('-NoProfile','-File',('"'+$worker+'"'),'-Task',$tasks[$i],'-Runtime',$runtimes[$i],'-ReviewDirectory',('"'+$review+'"'))
        $child = Start-Process -FilePath $pwsh -WindowStyle Hidden -ArgumentList $arguments -PassThru
        $children += $child
        if ($i -eq 0) {
            $deadline = [DateTime]::UtcNow.AddMinutes(5)
            while (!(Test-Path -LiteralPath (Join-Path $out 'server-ready.json'))) {
                if ($child.HasExited -or [DateTime]::UtcNow -gt $deadline) {throw 'Dedicated fixture server did not become ready'}
                Start-Sleep -Milliseconds 500
            }
        }
    }
    $deadline = [DateTime]::UtcNow.AddMinutes(20)
    while (@($children | Where-Object { -not $_.HasExited }).Count -gt 0) {
        foreach ($name in @('server-result.json','A-result.json','B-result.json')) {
            $path = Join-Path $out $name
            if (Test-Path -LiteralPath $path) {
                $receipt = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
                if (!$receipt.passed) { throw "Scenario receipt failed: $name, $($receipt.error)" }
            }
        }
        if ([DateTime]::UtcNow -gt $deadline) {throw 'Multiplayer topology exceeded its deadline'}
        Start-Sleep -Milliseconds 500
    }
    foreach ($child in $children) {if($child.ExitCode -ne 0){throw "Topology member failed: $($child.Id), exit $($child.ExitCode)"}}
    foreach ($name in @('server-result.json','A-result.json','B-result.json')) {
        $receipt = Get-Content -LiteralPath (Join-Path $out $name) -Raw | ConvertFrom-Json
        if (!$receipt.passed) {throw "Scenario receipt failed: $name"}
    }
    'multiplayer topology passed'
} finally {
    # Only fresh game processes for this topology's declared launch files are owned.
    $markers = @('bankServerRunProgramArgs.txt','bankClientARunProgramArgs.txt','bankClientBRunProgramArgs.txt')
    $ownedGames = @(if ($children.Count -gt 0) { Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {
        $candidate = $_
        $candidate.ProcessId -notin $priorJava -and $candidate.CommandLine -and
            $candidate.CommandLine.Contains($root) -and @($markers | Where-Object { $candidate.CommandLine.Contains($_) }).Count -gt 0
    } })
    foreach ($game in $ownedGames) { Stop-Process -Id $game.ProcessId -Force -ErrorAction SilentlyContinue }
    # Only children owned by this test orchestrator may be stopped after a failed session.
    foreach ($child in $children) {
        if (-not $child.HasExited) {
            $owned = Get-CimInstance Win32_Process | Where-Object ParentProcessId -eq $child.Id
            foreach ($process in $owned) {
                if ($process.CommandLine -like '*AlgoCraft*') { Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue }
            }
            Stop-Process -Id $child.Id -Force -ErrorAction SilentlyContinue
        }
    }
    foreach ($path in $saved.Keys) {
        if ($null -ne $saved[$path]) {[IO.File]::WriteAllBytes($path,$saved[$path])}
        elseif (Test-Path -LiteralPath $path) {Remove-Item -LiteralPath $path}
    }
    if ($saved.Count -gt 0) {
        $restored = foreach ($path in $saved.Keys) {
            if ($null -eq $saved[$path]) {!(Test-Path -LiteralPath $path)}
            else {([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([IO.File]::ReadAllBytes($path)))) -eq ([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($saved[$path])))}
        }
        [ordered]@{allRestored= !($restored -contains $false);paths=$saved.Keys} | ConvertTo-Json -Depth 4 |
            Set-Content -LiteralPath (Join-Path $out 'configuration-restored.json') -Encoding utf8NoBOM
    }
    Pop-Location
}
exit 0
