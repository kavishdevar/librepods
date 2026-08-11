<#
    LibrePods for Windows — one-shot installer.

    Installs BOTH kernel drivers (test-signed on the fly):
      • LibrePodsAAP  — opens the AirPods AAP L2CAP channel (battery, ANC, mic, …).
      • LibrePodsMic  — a virtual microphone so any app can use the AirPods mic.
    Then copies the daemon + WinUI app to %LOCALAPPDATA%\LibrePods and adds them to
    startup (the WinUI app launches minimised to the tray).

    RUN AS ADMINISTRATOR, and only AFTER you have:
      1. Backed up your BitLocker recovery key.
      2. Disabled Secure Boot in your firmware/BIOS.
      3. Enabled test signing:  bcdedit /set testsigning on   (then rebooted).

    Signing the drivers needs signtool.exe (from the Windows SDK/WDK). Everything
    else — driver packages, devcon, apps — is bundled in this folder.

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
$haveMic = (Test-Path $mic.sys) -and (Test-Path $mic.inf) -and (Test-Path $devcon)

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

# ---- 2. sign the AAP driver; (re)generate + sign the mic catalog ------------
function Sign($path) { & $signtool sign /v /fd SHA256 /sm /s My /sha1 $cert.Thumbprint $path }
Write-Host '==> Signing LibrePodsAAP...'
Sign $aap.sys; Sign $aap.cat
if ($haveMic) {
    # The mic catalog is regenerated here (inf2cat) so it matches the shipped .sys,
    # then signed. inf2cat ships with the WDK; fall back to a bundled catalog if absent.
    $inf2cat = (Get-ChildItem 'C:\Program Files (x86)\Windows Kits\10\bin' -Recurse -Filter inf2cat.exe -EA SilentlyContinue | Select-Object -First 1).FullName
    if ($inf2cat) { & $inf2cat /driver:(Split-Path $mic.inf) /os:10_X64 | Out-Null }
    Write-Host '==> Signing LibrePodsMic...'
    Sign $mic.sys
    if (Test-Path $mic.cat) { Sign $mic.cat }
}

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
# The daemon owns the driver + AAP session + mic; the WinUI app is its IPC client.
Write-Host "==> Copying the apps to $dest"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Copy-Item (Join-Path $here 'librepodsd.exe') $dest -Force
foreach ($dll in 'avcodec-61.dll', 'avutil-59.dll', 'swresample-5.dll') {
    $p = Join-Path $here $dll
    if (Test-Path $p) { Copy-Item $p $dest -Force }
}
# The WinUI app ships as a self-contained folder.
if (Test-Path (Join-Path $here 'winui')) {
    Copy-Item (Join-Path $here 'winui') $dest -Recurse -Force
}

# ---- 6. auto-start at login -------------------------------------------------
# The daemon is the always-on background process (per-user, in the session — NOT a
# SYSTEM service, which couldn't touch the user's audio/mic). The WinUI app starts
# minimised to the tray (--tray) and is the UI; closing its window hides it back.
Write-Host '==> Adding the daemon + WinUI app to startup...'
$startup = [Environment]::GetFolderPath('Startup')
$ws = New-Object -ComObject WScript.Shell

$lnkd = $ws.CreateShortcut((Join-Path $startup 'LibrePods Daemon.lnk'))
$lnkd.TargetPath = Join-Path $dest 'librepodsd.exe'
$lnkd.WorkingDirectory = $dest
$lnkd.Description = 'LibrePods background daemon'
$lnkd.Save()

$winui = Join-Path $dest 'winui\librepods-winui.exe'
if (Test-Path $winui) {
    $lnk = $ws.CreateShortcut((Join-Path $startup 'LibrePods.lnk'))
    $lnk.TargetPath = $winui
    $lnk.Arguments = '--tray'
    $lnk.WorkingDirectory = Split-Path $winui
    $lnk.Description = 'LibrePods AirPods control'
    $lnk.Save()
}

Write-Host ''
Write-Host '==> Done. A reboot is needed to finish the driver install.'
Write-Host '    After reboot, connect your AirPods — the WinUI app (tray) shows battery'
Write-Host '    + Noise Control, and "AirPods …" appears as a microphone in Sound > Input.'
