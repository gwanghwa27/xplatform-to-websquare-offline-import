# Helper for BUILD-AND-VERIFY.cmd/.sh. NOT part of the Production converter.
# Scans generated XML under -OutputRoot and reports class/state policy invariants plus
# HOLD structural-class leakage. No external dependency (System.Xml / built-in regex only).
#
# Usage: powershell -NoProfile -File class-policy-check.ps1 -OutputRoot "<path>"
# Output (NAME=VALUE lines) + exit code 0 (PASS) / 1 (FAIL).

param(
    [Parameter(Mandatory = $true)]
    [string]$OutputRoot
)

$files = Get-ChildItem -Path $OutputRoot -Filter *.xml -Recurse -File
$allText = ($files | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8 }) -join "`n"

function Count-Matches($pattern, $text) {
    return ([regex]::Matches($text, $pattern)).Count
}

$btnCm = Count-Matches 'class="[^"]*\bbtn_cm\b[^"]*"' $allText
$wqGvw = Count-Matches '\bwq_gvw\b' $allText
$disabledSb = Count-Matches 'disabledClass="w2selectbox_disabled"' $allText

Write-Output ("BTN_CM=" + $btnCm)
Write-Output ("WQ_GVW=" + $wqGvw)
Write-Output ("DISABLED_SELECTBOX=" + $disabledSb)

$pass = ($btnCm -eq 12) -and ($wqGvw -eq 3) -and ($disabledSb -eq 4)
Write-Output ("INVARIANT_PASS=" + $pass)

$holdClasses = @('shbox','dfbox','tbbox','lybox','ly_column','ly_form','btnbox','pgtbox','rcard')
$leak = $false
foreach ($cls in $holdClasses) {
    $n = Count-Matches ('class="[^"]*\b' + [regex]::Escape($cls) + '\b[^"]*"') $allText
    Write-Output ("HOLD_" + $cls.ToUpper() + "=" + $n)
    if ($n -gt 0) { $leak = $true }
}
Write-Output ("HOLD_LEAK=" + $leak)

if ($pass -and (-not $leak)) {
    exit 0
} else {
    exit 1
}
