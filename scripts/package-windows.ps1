param([string]$Version = "0.3.0")
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

$InputDir = "target/windows-input"
$ImageDir = "target/windows-image"
$App = "$ImageDir/AlphaDesktop"
New-Item -ItemType Directory -Force $InputDir, $ImageDir, "release" | Out-Null
Copy-Item "target/alpha-desktop-$Version.jar" $InputDir

# This is a real Windows launcher and a private Java runtime, not a renamed JAR.
& jpackage --type app-image --name AlphaDesktop --app-version $Version `
    --input $InputDir --main-jar "alpha-desktop-$Version.jar" --main-class AlphaDesktop `
    --dest $ImageDir --vendor "Alpha Desktop" --description "Alpha-inspired desktop sandbox" `
    --add-modules "java.desktop,java.prefs,java.logging,java.xml,java.naming,jdk.unsupported" `
    --jlink-options "--strip-debug --no-header-files --no-man-pages" `
    --java-options "-Dfile.encoding=UTF-8"
if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed" }

# Test BOTH the bundled runtime and the actual generated Windows .exe.
& "$App/runtime/bin/java.exe" "-Djava.awt.headless=true" -jar "$App/app/alpha-desktop-$Version.jar" --check-runtime
if ($LASTEXITCODE -ne 0) { throw "Bundled Java / Vorbis smoke test failed" }
$Process = Start-Process -FilePath "$App/AlphaDesktop.exe" -ArgumentList "--check-runtime" -PassThru
if (-not $Process.WaitForExit(30000)) {
    $Process.Kill()
    throw "Windows EXE did not exit from runtime smoke test"
}
if ($Process.ExitCode -ne 0) { throw "Windows EXE smoke test failed: $($Process.ExitCode)" }

# Copy readme, decoder distributions, and source archives into the portable app.
Expand-Archive "release/alpha-desktop-$Version.zip" -DestinationPath "target/portable-docs" -Force
$Docs = "target/portable-docs/alpha-desktop-$Version"
Copy-Item "$Docs/third-party" "$App/third-party" -Recurse
Copy-Item README.md, RELEASE_NOTES.md $App

# The portable zip contains AlphaDesktop.exe + its runtime. Keep them together.
Compress-Archive -Path $App -DestinationPath "release/AlphaDesktop-$Version-windows-x64-portable.zip" -Force

# Hosted Windows 2022 runners provide WiX 3 for jpackage's native installer.
if ($env:WIX) { $env:Path = "$env:WIX/bin;$env:Path" }
& jpackage --type exe --name AlphaDesktop --app-version $Version --app-image $App `
    --dest release --vendor "Alpha Desktop" --description "Alpha-inspired desktop sandbox" `
    --win-per-user-install --win-dir-chooser --win-menu --win-shortcut `
    --win-upgrade-uuid "d5649b47-8517-4e7f-a89d-fd85396f9f50"
if ($LASTEXITCODE -ne 0) { throw "Windows installer packaging failed" }
$Installer = "release/AlphaDesktop-$Version.exe"
if (-not (Test-Path $Installer)) { throw "Windows EXE installer missing" }
# Confirm this is a Windows PE executable, not just a file with an .exe extension.
$Bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $Installer))
if ($Bytes[0] -ne 0x4D -or $Bytes[1] -ne 0x5A) { throw "Installer lacks Windows MZ signature" }
$PeOffset = [BitConverter]::ToInt32($Bytes, 0x3C)
if ($Bytes[$PeOffset] -ne 0x50 -or $Bytes[$PeOffset + 1] -ne 0x45) { throw "Installer lacks PE signature" }

$Files = Get-ChildItem release -File | Where-Object { $_.Extension -eq ".exe" -or $_.Name -like "*-windows-x64-portable.zip" }
$Lines = $Files | ForEach-Object { ((Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower()) + "  " + $_.Name }
$Lines | Set-Content -Encoding ascii "release/SHA256SUMS-windows.txt"
Write-Host "Windows launcher smoke test passed; installer and portable ZIP built."
