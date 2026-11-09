$ErrorActionPreference = "Continue"
$mcDir = "$env:APPDATA\.tlauncher\legacy\Minecraft\game"
$modsDir = Join-Path $mcDir "mods"
$disabledDir = Join-Path $mcDir "mods_disabled_conflicts"

Write-Host "=== DANG KIEM TRA VA CAP NHAT AUTOLOGIN CHO MODPACK MINECRAFT ===" -ForegroundColor Cyan

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

    # 2. Xoa ban autologin cu
    Get-ChildItem -Path $modsDir -Filter "*AutoLoginDialog*" -ErrorAction SilentlyContinue | Remove-Item -Force
    Get-ChildItem -Path $modsDir -Filter "*autologin-dialog*" -ErrorAction SilentlyContinue | Remove-Item -Force

    # 3. Nhan dien phien ban Minecraft cua modpack (26.1.2 hay 26.2)
    $is26_1_2 = $false
    $is26_2 = $false

    if (Get-ChildItem -Path $modsDir -Filter "*26.1.2*" -ErrorAction SilentlyContinue) {
        $is26_1_2 = $true
    } elseif (Get-ChildItem -Path $modsDir -Filter "*26.2*" -ErrorAction SilentlyContinue) {
        $is26_2 = $true
    }

    # Chon file JAR tuong ung trong build\libs\
    $jarToCopy = $null
    if ($is26_1_2) {
        Write-Host "[i] Phat hien modpack Minecraft 26.1.2" -ForegroundColor Cyan
        $jarToCopy = Get-ChildItem -Path "build\libs\AutoLoginDialog-26.1.2-*.jar" | Select-Object -First 1
        if (!$jarToCopy) {
            $jarToCopy = Get-ChildItem -Path "mc-26.1.2\build\libs\AutoLoginDialog-26.1.2-*.jar" | Select-Object -First 1
        }
    } else {
        Write-Host "[i] Phat hien modpack Minecraft 26.2" -ForegroundColor Cyan
        $jarToCopy = Get-ChildItem -Path "build\libs\AutoLoginDialog-26.2-*.jar" | Select-Object -First 1
        if (!$jarToCopy) {
            $jarToCopy = Get-ChildItem -Path "mc-26.2\build\libs\AutoLoginDialog-26.2-*.jar" | Select-Object -First 1
        }
    }

    if ($jarToCopy) {
        Copy-Item -Path $jarToCopy.FullName -Destination $modsDir -Force
        Write-Host "[+] Da cap nhat $($jarToCopy.Name) vao thu muc mods!" -ForegroundColor Green
    } else {
        Get-ChildItem -Path "build\libs\AutoLoginDialog-*.jar" | ForEach-Object {
            Copy-Item -Path $_.FullName -Destination $modsDir -Force
            Write-Host "[+] Da copy $($_.Name) vao thu muc mods!" -ForegroundColor Green
        }
    }
} else {
    Write-Host "[i] Khong tim thay thu muc mac dinh $modsDir. Hay copy file JAR trong build/libs/ vao thu muc mods cua ban." -ForegroundColor Gray
}

Write-Host "=== HOAN TAT CAP NHAT ===" -ForegroundColor Green
