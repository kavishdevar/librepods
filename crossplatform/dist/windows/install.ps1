<#
    LibrePods for Windows — one-shot installer.

    Installs the LibrePodsAAP kernel driver (test-signed), copies the apps to
    %LOCALAPPDATA%\LibrePods, and adds the tray app to startup.

    RUN AS ADMINISTRATOR, and only AFTER you have:
      1. Backed up your BitLocker recovery key.
      2. Disabled Secure Boot in your firmware/BIOS.
      3. Enabled test signing:  bcdedit /set testsigning on   (then rebooted).

    Usage (elevated):  .\install.ps1
#>
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$drv  = Join-Path $here 'driver'
$dest = Join-Path $env:LOCALAPPDATA 'LibrePods'

$sys = Join-Path $drv 'LibrePodsAAP.sys'
$cat = Join-Path $drv 'librepodsaap.cat'
$inf = Join-Path $drv 'LibrePodsAAP.inf'
foreach ($f in @($sys, $cat, $inf)) { if (-not (Test-Path $f)) { throw "Missing $f" } }

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

$signtool = (Get-ChildItem 'C:\Program Files (x86)\Windows Kits\10\bin' -Recurse -Filter signtool.exe |
    Where-Object { $_.FullName -match 'x64' } | Select-Object -First 1).FullName
Write-Host "==> Signing the driver with $signtool"
& $signtool sign /v /fd SHA256 /sm /s My /sha1 $cert.Thumbprint $sys
& $signtool sign /v /fd SHA256 /sm /s My /sha1 $cert.Thumbprint $cat

Write-Host '==> Removing any previously installed LibrePodsAAP package...'
$oem = $null
pnputil /enum-drivers | ForEach-Object {
    if ($_ -match 'Published Name\s*:\s*(oem\d+\.inf)') { $oem = $matches[1] }
    if ($_ -match 'Original Name\s*:\s*LibrePodsAAP\.inf' -and $oem) {
        pnputil /delete-driver $oem /uninstall /force | Out-Null
    }
}

Write-Host '==> Installing the driver...'
pnputil /add-driver $inf /install

Write-Host "==> Copying the apps to $dest"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Copy-Item (Join-Path $here 'librepods-tray.exe') $dest -Force
Copy-Item (Join-Path $here 'librepods.exe') $dest -Force

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
Write-Host '    After reboot, connect your AirPods — the tray shows battery + ANC.'
Write-Host '    Tray menu -> "Open App" launches the full window (volume slider, etc.).'
