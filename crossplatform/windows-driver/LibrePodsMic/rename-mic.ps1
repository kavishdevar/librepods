<#
    rename-mic.ps1 - set the LibrePodsMic virtual microphone's display name (e.g.
    the connected AirPods' name). RUN AS ADMINISTRATOR.

        .\rename-mic.ps1 "AirPods Pro de Pedro"

    MMDevices property values are REG_BINARY serialized PROPVARIANTs (4-byte type
    tag + UTF-16 string), not plain strings - this reads/writes them correctly.
    It matches the AudioCodec capture endpoint by DeviceDesc/FriendlyName, sets
    PKEY_Device_FriendlyName, and refreshes the audio endpoint service so apps
    pick it up without a reboot. (Plan B for when IPolicyConfig isn't available.)
#>
param([Parameter(Mandatory)][string]$Name)
$ErrorActionPreference = 'Stop'

$base    = 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\MMDevices\Audio\Capture'
$descKey = '{a45c254e-df1c-4efd-8020-67d146a850e0},2'   # PKEY_Device_DeviceDesc
$fnKey   = '{a45c254e-df1c-4efd-8020-67d146a850e0},14'  # PKEY_Device_FriendlyName

# Decode a REG_BINARY PROPVARIANT string value (skip the 4-byte type tag).
function Decode-PropStr($bytes) {
    if (-not $bytes -or $bytes.Length -le 4) { return '' }
    $s = [System.Text.Encoding]::Unicode.GetString($bytes[4..($bytes.Length - 1)])
    return $s.TrimEnd([char]0)
}
# Encode a string as a VT_LPWSTR PROPVARIANT REG_BINARY (0x1F tag + UTF-16 + NUL).
function Encode-PropStr([string]$s) {
    $prefix = [byte[]](0x1F, 0x00, 0x00, 0x00)
    $data   = [System.Text.Encoding]::Unicode.GetBytes($s + [char]0)
    return $prefix + $data
}

$target = $null
Write-Host '==> Capture endpoints found:'
Get-ChildItem $base | ForEach-Object {
    $props = Join-Path $_.PSPath 'Properties'
    $p = Get-ItemProperty -Path $props -ErrorAction SilentlyContinue
    $desc = if ($p.$descKey) { Decode-PropStr $p.$descKey } else { '' }
    $fn   = if ($p.$fnKey)   { Decode-PropStr $p.$fnKey }   else { '' }
    Write-Host ("    [{0}] desc='{1}' friendly='{2}'" -f $_.PSChildName, $desc, $fn)
    if ($desc -like '*AudioCodec*' -or $fn -like '*AudioCodec*') {
        $target = $props
    }
}

if (-not $target) {
    Write-Host 'No AudioCodec capture endpoint matched. Is the LibrePodsMic driver installed?'
    exit 1
}

Set-ItemProperty -Path $target -Name $fnKey -Value (Encode-PropStr $Name) -Type Binary
Write-Host "==> Set FriendlyName -> '$Name'"

Write-Host '==> Refreshing the audio endpoint service (brief audio drop)...'
Restart-Service -Name AudioEndpointBuilder -Force
Write-Host '==> Done. Check Sound settings / Discord - it should show the new name.'
