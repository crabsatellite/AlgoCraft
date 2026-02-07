$files = Get-ChildItem "e:\MinecraftDev\archived\AlgoCraft\question_bank\official\p*.json"
$broken = @()
foreach($f in $files) {
    try {
        $null = Get-Content $f.FullName -Raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        $broken += $f.Name
        Write-Host "BROKEN: $($f.Name) - $($_.Exception.Message)"
    }
}
if ($broken.Count -eq 0) {
    Write-Host "All JSON files are valid!"
} else {
    Write-Host "`nTotal broken files: $($broken.Count)"
}
