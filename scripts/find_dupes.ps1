$files = Get-ChildItem "e:\MinecraftDev\archived\AlgoCraft\question_bank\official\p*.json"
$data = @()
foreach($f in $files){
    $j = Get-Content $f.FullName -Raw | ConvertFrom-Json
    $data += [PSCustomObject]@{Id=$j.id; Title=$j.title}
}
$dupes = $data | Group-Object Title | Where-Object { $_.Count -gt 1 }
foreach($g in $dupes){
    Write-Host "---"
    foreach($item in $g.Group){
        Write-Host ("P" + $item.Id + ": " + $item.Title)
    }
}
Write-Host "---"
Write-Host ("Total duplicate groups: " + $dupes.Count)
