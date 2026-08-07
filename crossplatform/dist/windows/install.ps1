<#
    LibrePods for Windows — one-shot installer.

    Installs BOTH kernel drivers (test-signed):
      • LibrePodsAAP  — opens the AirPods AAP L2CAP channel (battery, ANC, …).
      • LibrePodsMic  — a virtual microphone so apps can use the AirPods mic.
    Then copies the apps to %LOCALAPPDATA%\LibrePods, registers the elevated
    mic-rename task, and adds the tray app to startup.

    RUN AS ADMINISTRATOR, and only AFTER you have:
      1. Backed up your BitLocker recovery key.
      2. Disabled Secure Boot in your firmware/BIOS.
      3. Enabled test signing:  bcdedit /set testsigning on   (then rebooted).

    Usage (elevated):  .\install.ps1
#>
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$dest = Join-Path $env:LOCALAPPDATA 'LibrePods'

# ---- locate tools -----------------------------------------------------------
$signtool = (Get-ChildItem 'C:\Program Files (x86)\Windows Kits\10\bin' -Recurse -Filter signtool.exe -EA SilentlyContinue |
    Where-Object { $_.FullName -match 'x64' } | Select-Object -First 1).FullName
if (-not $signtool) { throw 'signtool.exe not found — install the Windows SDK/WDK (needed to test-sign the drivers).' }
$devcon = Join-Path $here 'tools\devcon.exe'   # bundled; creates the ROOT\AudioCodec device

# ---- driver files -----------------------------------------------------------
$aap = @{ sys = Join-Path $here 'driver\LibrePodsAAP.sys'; cat = Join-Path $here 'driver\librepodsaap.cat'; inf = Join-Path $here 'driver\LibrePodsAAP.inf' }
$mic = @{ sys = Join-Path $here 'driver-mic\AudioCodec.sys'; cat = Join-Path $here 'driver-mic\audiocodec.cat'; inf = Join-Path $here 'driver-mic\AudioCodec.inf' }
foreach ($f in $aap.Values) { if (-not (Test-Path $f)) { throw "Missing $f" } }
$haveMic = (Test-Path $mic.sys) -and (Test-Path $mic.cat) -and (Test-Path $mic.inf) -and (Test-Path $devcon)

# ---- 1. test code-signing cert, trusted for driver loading ------------------
Write-Host '==> Creating + trusting a test code-signing certificate...'
$cert = New-SelfSignedCertificate -Type CodeSigningCert `
    -Subject 'CN=LibrePods Test Cert' `
    -CertStoreLocation Cert:\LocalMachine\My `
    -KeyUsage DigitalSignature -KeyExportPolicy Exportable
$store = Get-Item "Cert:\LocalMachine\My\$($cert.Thumbprint)"
foreach ($name in 'Root', 'TrustedPublisher') {
    $s = New-Object System.Security.Cryptography.X509Certificates.X509Store($name, 'LocalMachine')
    $s.Open('ReadWrite'); $s.Add($store); $s.Close()
}

# ---- 2. sign both drivers with that cert ------------------------------------
function Sign($path) { & $signtool sign /v /fd SHA256 /sm /s My /sha1 $cert.Thumbprint $path }
Write-Host '==> Signing LibrePodsAAP...'
Sign $aap.sys; Sign $aap.cat
if ($haveMic) { Write-Host '==> Signing LibrePodsMic...'; Sign $mic.sys; Sign $mic.cat }

# ---- 3. install LibrePodsAAP (PnP profile driver, via pnputil) --------------
Write-Host '==> Removing any previously installed LibrePodsAAP package...'
$oem = $null
pnputil /enum-drivers | ForEach-Object {
    if ($_ -match 'Published Name\s*:\s*(oem\d+\.inf)') { $oem = $matches[1] }
    if ($_ -match 'Original Name\s*:\s*LibrePodsAAP\.inf' -and $oem) {
        pnputil /delete-driver $oem /uninstall /force | Out-Null
    }
}
Write-Host '==> Installing LibrePodsAAP...'
pnputil /add-driver $aap.inf /install

# ---- 4. install LibrePodsMic (ROOT-enumerated device, via devcon) -----------
if ($haveMic) {
    Write-Host '==> Removing any existing ROOT\AudioCodec (mic) device...'
    & $devcon remove 'ROOT\AudioCodec' 2>&1 | Out-Null
    Start-Sleep -Seconds 1
    Write-Host '==> Installing LibrePodsMic (virtual microphone)...'
    & $devcon install $mic.inf 'ROOT\AudioCodec'
} else {
    Write-Host '==> (Skipping LibrePodsMic — driver-mic\ or tools\devcon.exe not bundled.)'
}

# ---- 5. copy the apps -------------------------------------------------------
Write-Host "==> Copying the apps to $dest"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Copy-Item (Join-Path $here 'librepods-tray.exe') $dest -Force
Copy-Item (Join-Path $here 'librepods.exe') $dest -Force

# lp-mic-rename + its elevated on-demand task: lets the (unelevated) tray rename
# the virtual mic to the connected device's name without a UAC prompt.
$renameExe = Join-Path $here 'lp-mic-rename.exe'
if (Test-Path $renameExe) {
    Copy-Item $renameExe $dest -Force
    $renameDest = Join-Path $dest 'lp-mic-rename.exe'
    Write-Host '==> Registering the elevated mic-rename task...'
    $taskName = 'LibrePods Rename Mic'
    $action    = New-ScheduledTaskAction -Execute $renameDest
    $principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" `
        -LogonType Interactive -RunLevel Highest
    $settings  = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries `
        -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Minutes 2) `
        -StartWhenAvailable
    Register-ScheduledTask -TaskName $taskName -Action $action -Principal $principal `
        -Settings $settings -Description 'Rename the LibrePods virtual mic to the connected device name.' `
        -Force | Out-Null
}

# ---- 6. auto-start the tray at login ----------------------------------------
Write-Host '==> Adding the tray app to startup...'
$startup = [Environment]::GetFolderPath('Startup')
$ws = New-Object -ComObject WScript.Shell
$lnk = $ws.CreateShortcut((Join-Path $startup 'LibrePods.lnk'))
$lnk.TargetPath = Join-Path $dest 'librepods-tray.exe'
$lnk.WorkingDirectory = $dest
$lnk.Description = 'LibrePods AirPods control'
$lnk.Save()

Write-Host ''
Write-Host '==> Done. A reboot is needed to finish the driver install.'
Write-Host '    After reboot, connect your AirPods — the tray shows battery + ANC,'
Write-Host '    and "AirPods …" appears as a microphone in Sound > Input.'
Write-Host '    Tray menu -> "Open App" launches the full window (volume slider, etc.).'
