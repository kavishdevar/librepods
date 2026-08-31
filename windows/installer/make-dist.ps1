<#
    Assemble a ready-to-install LibrePods-dist folder from THIS repository.

    Why this exists: the dist folder had drifted into a second, older copy of the
    installer (its install.ps1 still deployed the retired iced app and tray, and
    knew nothing about the daemon or the WinUI app), and every fix had to be made
    twice. The repository is the single source of truth; this script is the only
    supported way to produce a dist from it.

    Everything except the two built artifacts comes straight out of the repo:
    the installer, the recovery script, devcon, and both prebuilt driver packages.
    The two that must be built first:

        windows\daemon                 cargo build --release --target x86_64-pc-windows-gnu
        windows\winui\LibrePods.WinUI  MSBuild -t:Publish -p:Configuration=Release -p:Platform=x64

    Note the WinUI app needs VISUAL STUDIO's MSBuild, not `dotnet publish`: the
    WindowsAppSDK PRI step fails on the plain .NET SDK with
    "Microsoft.Build.Packaging.Pri.Tasks.dll ... could not be loaded". And its
    publish output DROPS librepods-winui.pri, the app's own resource index, which
    this script copies back in - without it the app starts with no strings and no
    icons.

    Usage:  .\make-dist.ps1 [-Out <path>]
#>
[CmdletBinding()]
param(
    # Where to write the dist. Defaults to <repo parent>\LibrePods-dist, which is
    # where the project keeps it when the sub-projects are grouped under one root.
    [string]$Out
)

$ErrorActionPreference = 'Stop'
$installer = $PSScriptRoot
$win       = Split-Path -Parent $installer          # ...\windows
$repo      = Split-Path -Parent $win                # the repo root
if (-not $Out) { $Out = Join-Path (Split-Path -Parent $repo) 'LibrePods-dist' }

$daemonExe = Join-Path $win 'daemon\target\x86_64-pc-windows-gnu\release\librepodsd.exe'
$winuiOut  = Join-Path $win 'winui\LibrePods.WinUI\bin\x64\Release\net10.0-windows10.0.19041.0\win-x64'
$winuiPub  = Join-Path $winuiOut 'publish'

foreach ($p in @($daemonExe, $winuiPub)) {
    if (-not (Test-Path $p)) { throw "Not built yet: $p`nSee the header of this script for the build commands." }
}

Write-Host "==> Assembling $Out"
New-Item -ItemType Directory -Force -Path $Out | Out-Null
foreach ($sub in 'driver', 'driver-mic', 'tools', 'winui') {
    $d = Join-Path $Out $sub
    if (Test-Path $d) { Remove-Item $d -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}

# ---- installer + recovery script -------------------------------------------
Copy-Item (Join-Path $installer 'install.ps1')    $Out -Force
Copy-Item (Join-Path $installer 'fix-driver.ps1') $Out -Force
Copy-Item (Join-Path $installer 'tools\*')        (Join-Path $Out 'tools') -Recurse -Force

# ---- driver packages (prebuilt, so no WDK is needed to install) -------------
Copy-Item (Join-Path $win 'drivers\aap\prebuilt\*') (Join-Path $Out 'driver') -Force
# The mic package ships without a catalog on purpose: install.ps1 regenerates it
# with inf2cat so it always matches the shipped .sys, then signs it.
Copy-Item (Join-Path $win 'drivers\mic\prebuilt\*') (Join-Path $Out 'driver-mic') -Force
Get-ChildItem (Join-Path $Out 'driver'), (Join-Path $Out 'driver-mic') -Filter 'README.md' |
    Remove-Item -Force -ErrorAction SilentlyContinue

# ---- daemon + the FFmpeg runtime it links against ---------------------------
Copy-Item $daemonExe $Out -Force
Copy-Item (Join-Path $win 'daemon\vendor\ffmpeg\bin\*.dll') $Out -Force

# ---- WinUI app (unpackaged + self-contained: a whole folder) ----------------
Copy-Item (Join-Path $winuiPub '*') (Join-Path $Out 'winui') -Recurse -Force
Copy-Item (Join-Path $winuiOut 'librepods-winui.pri') (Join-Path $Out 'winui') -Force

$size = '{0:N0} MB' -f ((Get-ChildItem $Out -Recurse -File | Measure-Object Length -Sum).Sum / 1MB)
$count = (Get-ChildItem $Out -Recurse -File).Count
Write-Host "==> Done: $count files, $size" -ForegroundColor Green
Write-Host "    Install with (elevated):  cd '$Out'; .\install.ps1" -ForegroundColor DarkGray
