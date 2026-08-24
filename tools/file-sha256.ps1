# Helper for build-pipeline-trace.bat. NOT part of the Production converter.
# Invoked with -File so no path or logic is embedded in a quoted -Command string.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File file-sha256.ps1 -TargetPath "<file>"
# Output: single line, the SHA-256 hash of the file.

param(
    [Parameter(Mandatory = $true)]
    [string]$TargetPath
)

$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $TargetPath
Write-Output $hash.Hash
