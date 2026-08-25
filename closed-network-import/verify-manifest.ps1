# Helper for BUILD-AND-VERIFY.cmd. NOT part of the Production converter.
# Verifies every file listed in -ManifestPath (format: "<sha256>  <relative-path>" per line,
# same convention as sha256sum -c) matches its SHA-256 under -RepoRoot. No external dependency
# (Get-FileHash is a PowerShell built-in).
#
# Usage: powershell -NoProfile -File verify-manifest.ps1 -RepoRoot "<path>" -ManifestPath "<path>"
# Output: one line per mismatch/missing file, then MANIFEST_TOTAL=N / MANIFEST_MISMATCH=N.
# Exit code 0 (all match) / 1 (one or more mismatches or missing files).

param(
    [Parameter(Mandatory = $true)]
    [string]$RepoRoot,
    [Parameter(Mandatory = $true)]
    [string]$ManifestPath
)

$lines = Get-Content -LiteralPath $ManifestPath -Encoding UTF8
$total = 0
$mismatch = 0

foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0) { continue }
    $m = [regex]::Match($trimmed, '^([0-9a-fA-F]{64})\s+\*?(.+)$')
    if (-not $m.Success) { continue }
    $expected = $m.Groups[1].Value.ToLower()
    $relPath = $m.Groups[2].Value
    $total++
    $fullPath = Join-Path $RepoRoot $relPath
    if (-not (Test-Path -LiteralPath $fullPath)) {
        Write-Output ("MISSING: " + $relPath)
        $mismatch++
        continue
    }
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $fullPath).Hash.ToLower()
    if ($actual -ne $expected) {
        Write-Output ("MISMATCH: " + $relPath + " expected=" + $expected + " actual=" + $actual)
        $mismatch++
    }
}

Write-Output ("MANIFEST_TOTAL=" + $total)
Write-Output ("MANIFEST_MISMATCH=" + $mismatch)

if ($mismatch -gt 0) { exit 1 } else { exit 0 }
