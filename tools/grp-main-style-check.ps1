# Helper for build-pipeline-trace.bat. NOT part of the Production converter, and not an
# external dependency -- ships alongside build-pipeline-trace.bat in this same tools/ directory,
# invoked with -File so cmd does not need to embed any of this logic (or the target file path)
# inside a quoted -Command string.
#
# Usage: powershell -NoProfile -File grp-main-style-check.ps1 -TargetPath "<file>"
# Output (three lines, parsed by the caller as NAME=VALUE):
#   RESULTAREA=<grp_resultArea element text, or (not found)>
#   GRPMAIN=<grp_main element text, or (not found)>
#   EMPTY=<YES|NO|N/A>

param(
    [Parameter(Mandatory = $true)]
    [string]$TargetPath
)

$content = Get-Content -LiteralPath $TargetPath -Raw -Encoding UTF8

$resultAreaMatch = [regex]::Match($content, 'id=.grp_resultArea.[^/]*')
if ($resultAreaMatch.Success) {
    $resultAreaText = $resultAreaMatch.Value
} else {
    $resultAreaText = '(not found)'
}

$mainMatch = [regex]::Match($content, 'id=.grp_main.[^/]*')
if ($mainMatch.Success) {
    $mainText = $mainMatch.Value
} else {
    $mainText = '(not found)'
}

if ($mainMatch.Success) {
    if ($mainMatch.Value -match 'style=""') {
        $empty = 'YES'
    } else {
        $empty = 'NO'
    }
} else {
    $empty = 'N/A'
}

Write-Output ('RESULTAREA=' + $resultAreaText)
Write-Output ('GRPMAIN=' + $mainText)
Write-Output ('EMPTY=' + $empty)
