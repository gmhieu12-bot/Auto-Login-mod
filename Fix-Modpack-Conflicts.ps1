$ErrorActionPreference = "Continue"
$mcDir = "$env:APPDATA\.tlauncher\legacy\Minecraft\game"
$modsDir = Join-Path $mcDir "mods"
$disabledDir = Join-Path $mcDir "mods_disabled_conflicts"

Write-Host "=== DANG CAP NHAT AUTOLOGIN v1.4.0 CHO MINECRAFT 26.1.2 (DA PASS UNIT TEST) VA DON MOD XUNG DOT ===" -ForegroundColor Cyan

if (Test-Path $modsDir) {
    New-Item -ItemType Directory -Force -Path $disabledDir | Out-Null

    # 1. Di chuyen cac mod gay crash JVM Native (c2me-opts-accel-opencl) va loi Mixin (punchy_puppies)
    $conflictPatterns = @("*c2me-opts-accel-opencl*", "*c2me*opts-accel-opencl*", "*punchy_puppies*")
    foreach ($pattern in $conflictPatterns) {
        Get-ChildItem -Path $modsDir -Filter $pattern -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "[!] Phat hien mod xung dot: $($_.Name) -> Chuyen sang mods_disabled_conflicts/" -ForegroundColor Yellow
            Move-Item -Path $_.FullName -Destination (Join-Path $disabledDir $_.Name) -Force
        }
    }

    # 2. Xoa ban autologin-dialog cu va copy ban 26.1.2 moi nhat vao mods/
    Get-ChildItem -Path $modsDir -Filter "*AutoLoginDialog*" -ErrorAction SilentlyContinue | Remove-Item -Force
    Get-ChildItem -Path $modsDir -Filter "*autologin-dialog*" -ErrorAction SilentlyContinue | Remove-Item -Force

    $builtJar = Get-ChildItem -Path "build\libs\AutoLoginDialog-26.1.2-*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($builtJar) {
        Copy-Item -Path $builtJar.FullName -Destination $modsDir -Force
        Write-Host "[+] Da cap nhat $($builtJar.Name) vao thu muc mods!" -ForegroundColor Green
    }
} else {
    Write-Host "[i] Khong tim thay thu muc mac dinh $modsDir. Hay copy file JAR trong build/libs/ vao thu muc mods cua ban." -ForegroundColor Gray
}

Write-Host "=== HOAN TAT CAP NHAT v1.4.0 (26.1.2) ===" -ForegroundColor Green
