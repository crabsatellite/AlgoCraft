param(
    [ValidateSet('build','gradle')][string]$Action = 'build',
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArguments
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$tasks = @()
if ($Action -eq 'build') { $tasks += 'build' }
$tasks += $GradleArguments
Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @tasks
    exit $LASTEXITCODE
} finally { Pop-Location }
