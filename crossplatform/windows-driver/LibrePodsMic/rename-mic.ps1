<#
    rename-mic.ps1 - set the LibrePodsMic virtual microphone's display name (e.g.
    the connected AirPods' name), the reliable registry way. RUN AS ADMINISTRATOR.

        .\rename-mic.ps1 "AirPods Pro de Pedro"

    Writes PKEY_Device_FriendlyName on the AudioCodec capture endpoint and
    refreshes the audio endpoint service so apps pick it up without a reboot.
    (Plan B for when the IPolicyConfig runtime path isn't available.)
#>
param([Parameter(Mandatory)][string]$Name)
$ErrorActionPreference = 'Stop'

$base    = 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\MMDevices\Audio\Capture'
$descKey = '{a45c254e-df1c-4efd-8020-67d146a850e0},2'   # PKEY_Device_DeviceDesc
$fnKey   = '{a45c254e-df1c-4efd-8020-67d146a850e0},14'  # PKEY_Device_FriendlyName

$found = $false
Get-ChildItem $base | ForEach-Object {
    $props = Join-Path $_.PSPath 'Properties'
    try { $desc = (Get-ItemProperty -Path $props -Name $descKey -ErrorAction Stop).$descKey }
    catch { $desc = $null }
    if ($desc -like '*AudioCodec*') {
        Set-ItemProperty -Path $props -Name $fnKey -Value $Name -Type String
        Write-Host "==> Renamed '$desc' -> '$Name'"
        $found = $true
    }
}

if (-not $found) {
    Write-Host 'AudioCodec capture endpoint not found. Is the LibrePodsMic driver installed?'
    exit 1
}

Write-Host '==> Refreshing the audio endpoint service (brief audio drop)...'
Restart-Service -Name AudioEndpointBuilder -Force
Write-Host '==> Done. Check Sound settings / Discord - it should show the new name.'
