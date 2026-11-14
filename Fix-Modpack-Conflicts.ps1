$ErrorActionPreference = "Continue"
$mcDir = "$env:APPDATA\.tlauncher\legacy\Minecraft\game"
$modsDir = Join-Path $mcDir "mods"
$disabledDir = Join-Path $mcDir "mods_disabled_conflicts"

Write-Host "=== DANG KIEM TRA VA CAP NHAT AUTOLOGIN CHO MODPACK MINECRAFT ===" -ForegroundColor Cyan

# Lay version tu gradle.properties
$modVersion = "1.5.0"
if (Test-Path "gradle.properties") {
    $props = Get-Content -Path "gradle.properties" -Raw
    if ($props -match "mod_version\s*=\s*(.+)") {
        $modVersion = $matches[1].Trim()
    }
}

if (Test-Path $modsDir) {
    New-Item -ItemType Directory -Force -Path $disabledDir | Out-Null

    # 1. Di chuyen cac mod gay crash JVM Native (c2me-opts-accel-opencl) va loi Mixin (punchy_puppies)
    $conflictPatterns = @("*c2me-opts-accel-opencl*", "*c2me*opts-accel-opencl*", "*punchy_puppies*")
    foreach ($pattern in $conflictPatterns) {
        Get-ChildItem -Path $modsDir -Filter $pattern -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "[!] Phat hien mod xung dot: $($_.Name) -> Chuyen sang mods_disabled_conflicts/" -ForegroundColor Yellow
            Move-Item -Path $_.FullName -Destination (Join-Path $disabledDir $_.Name) -Force -ErrorAction SilentlyContinue
        }
    }

    # 2. Xu ly mod trung lap
    if ((Test-Path (Join-Path $modsDir "ukus-armor-hud-fabric-0.11.0+mc26.1.jar")) -and (Test-Path (Join-Path $modsDir "[vi_vn]-ukus-armor-hud-fabric-0.11.0mc26.1.jar"))) {
        Write-Host "[!] Phat hien file trung lap: [vi_vn]-ukus-armor-hud -> Chuyen sang mods_disabled_conflicts/" -ForegroundColor Yellow
        Move-Item -Path (Join-Path $modsDir "[vi_vn]-ukus-armor-hud-fabric-0.11.0mc26.1.jar") -Destination (Join-Path $disabledDir "[vi_vn]-ukus-armor-hud-fabric-0.11.0mc26.1.jar") -Force -ErrorAction SilentlyContinue
    }

    # 3. Nhan dien phien ban Minecraft cua modpack (26.1.2 hay 26.2)
    $is26_1_2 = $false
    $is26_2 = $false

    if (Get-ChildItem -Path $modsDir -Filter "*26.1.2*" -ErrorAction SilentlyContinue) {
        $is26_1_2 = $true
    } elseif (Get-ChildItem -Path $modsDir -Filter "*26.2*" -ErrorAction SilentlyContinue) {
        $is26_2 = $true
    }

    # Chon file JAR tuong ung trong build\libs\
    $targetJarName = if ($is26_1_2) { "AutoLoginDialog-26.1.2-$modVersion.jar" } else { "AutoLoginDialog-26.2-$modVersion.jar" }
    $jarToCopy = Get-ChildItem -Path "build\libs\$targetJarName" -ErrorAction SilentlyContinue | Select-Object -First 1

    if (!$jarToCopy) {
        $jarToCopy = Get-ChildItem -Path "build\libs\AutoLoginDialog-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    }

    # 4. Xoa ban autologin cu (neu khong bi tien trinh Minecraft dang khoa)
    Get-ChildItem -Path $modsDir -Filter "*AutoLoginDialog*" -ErrorAction SilentlyContinue | ForEach-Object {
        $oldFile = $_
        if ($oldFile.Name -ne $targetJarName) {
            try {
                Remove-Item -Path $oldFile.FullName -Force -ErrorAction Stop
                Write-Host "[-] Da go bo ban cu: $($oldFile.Name)" -ForegroundColor Yellow
            } catch {
                Write-Host "[i] File $($oldFile.Name) dang duoc Minecraft mo. Khi ban thoat game se tu dong ap dung ban moi ($targetJarName)!" -ForegroundColor Yellow
            }
        }
    }

    if ($jarToCopy) {
        Copy-Item -Path $jarToCopy.FullName -Destination (Join-Path $modsDir $jarToCopy.Name) -Force
        Write-Host "[+] Da cap nhat $($jarToCopy.Name) vao thu muc mods!" -ForegroundColor Green
    }
} else {
    Write-Host "[i] Khong tim thay thu muc mac dinh $modsDir. Hay copy file JAR trong build/libs/ vao thu muc mods cua ban." -ForegroundColor Gray
}

Write-Host "=== HOAN TAT CAP NHAT (v$modVersion) ===" -ForegroundColor Green
