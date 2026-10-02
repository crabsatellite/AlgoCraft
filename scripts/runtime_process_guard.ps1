[CmdletBinding()]
param(
    [switch]$FailIfDetected,
    [string]$ExcludeProcessIdsCsv = '',
    [string]$IncludePathMarkersCsv = ''
)

Set-StrictMode -Version Latest

function Get-MinecraftRuntimeProcesses {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Processes,

        [int[]]$ExcludeProcessIds = @(),

        [string[]]$IncludePathMarkers = @()
    )

    $runtimeMatches = [Collections.Generic.List[object]]::new()
    foreach ($process in $Processes) {
        if ($ExcludeProcessIds -contains [int]$process.ProcessId) {
            continue
        }
        $name = [string]$process.Name
        $commandLine = [string]$process.CommandLine
        if ($name -notmatch '^(java|javaw)\.exe$' -or
                [string]::IsNullOrWhiteSpace($commandLine)) {
            continue
        }
        $marker = switch -Regex ($commandLine) {
            '(?i)gradle-wrapper\.jar.*\b(runClient|runServer|runGameTestServer|clientGameTest|serverGameTest)\b' {
                'gradle-minecraft-runtime-task'; break
            }
            '(?i)cpw\.mods\.bootstraplauncher\.BootstrapLauncher.*--launchTarget\s+\S*(client|server|game[test]*)\S*' {
                'forge-bootstrap-runtime'; break
            }
            '(?i)net\.minecraft\.(client\.main|server)\.Main' {
                'mojang-runtime-main'; break
            }
            '(?i)(fabric|quilt).*loader.*launch.*knot\.Knot(Client|Server)' {
                'knot-runtime-main'; break
            }
            '(?i)fabric-server-launch\.jar' {
                'fabric-server-runtime'; break
            }
            default { '' }
        }
        if (-not [string]::IsNullOrWhiteSpace($marker)) {
            $scopedMarkers = @($IncludePathMarkers | Where-Object {
                -not [string]::IsNullOrWhiteSpace($_)
            })
            if ($scopedMarkers.Count -gt 0) {
                $matchesScope = $false
                foreach ($pathMarker in $scopedMarkers) {
                    if ($commandLine.IndexOf(
                            $pathMarker,
                            [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                        $matchesScope = $true
                        break
                    }
                }
                if (-not $matchesScope) { continue }
            }
            $runtimeMatches.Add([pscustomobject]@{
                processId = [int]$process.ProcessId
                name = $name
                marker = $marker
            })
        }
    }
    return @($runtimeMatches | Sort-Object processId -Unique)
}

if ($MyInvocation.InvocationName -ne '.') {
    try {
        $excluded = if ([string]::IsNullOrWhiteSpace($ExcludeProcessIdsCsv)) {
            @()
        } else {
            @($ExcludeProcessIdsCsv -split ',' | ForEach-Object { [int]$_ })
        }
        $inventory = @(Get-CimInstance Win32_Process -ErrorAction Stop)
        $byProcessId = @{}
        foreach ($process in $inventory) {
            $byProcessId[[int]$process.ProcessId] = $process
        }
        $expandedExcluded = [Collections.Generic.HashSet[int]]::new()
        foreach ($rootProcessId in $excluded) {
            $cursorProcessId = [int]$rootProcessId
            while ($cursorProcessId -gt 0 -and $expandedExcluded.Add($cursorProcessId)) {
                $cursor = $byProcessId[$cursorProcessId]
                if ($null -eq $cursor) { break }
                $cursorProcessId = [int]$cursor.ParentProcessId
            }
        }
        $familyExpanded = $true
        while ($familyExpanded) {
            $familyExpanded = $false
            foreach ($process in $inventory) {
                if ($expandedExcluded.Contains([int]$process.ParentProcessId) `
                        -and $expandedExcluded.Add([int]$process.ProcessId)) {
                    $familyExpanded = $true
                }
            }
        }
        $javaInventory = @($inventory | Where-Object {
            $_.Name -match '^(java|javaw)\.exe$'
        })
        $active = @(Get-MinecraftRuntimeProcesses -Processes $javaInventory `
            -ExcludeProcessIds @($expandedExcluded) `
            -IncludePathMarkers @($IncludePathMarkersCsv -split ',' |
                Where-Object { $_ }))
        [pscustomobject]@{
            processInventoryChecked = $true
            activeRuntimeProcesses = @($active)
        } | ConvertTo-Json -Depth 6 -Compress | Write-Output
        if ($FailIfDetected -and $active.Count -gt 0) { exit 77 }
    } catch {
        Write-Error "failed to inspect machine Minecraft runtime processes: $($_.Exception.Message)"
        exit 78
    }
}
