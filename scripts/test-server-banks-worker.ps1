param([string]$Task,[string]$Runtime,[string]$ReviewDirectory = 'build/review/server-banks-2026-10-01')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $root
try {
    $arguments=@('gradle',$Task,('-PalgocraftTestRuntimeDirectory='+ (Join-Path $root $Runtime)),'--no-daemon','--console=plain')
    $review = if ([IO.Path]::IsPathRooted($ReviewDirectory)) { $ReviewDirectory } else { Join-Path $root $ReviewDirectory }
    & ./mod-build.ps1 @arguments *> (Join-Path $review ($Task+'.log'))
    exit $LASTEXITCODE
} finally {Pop-Location}
