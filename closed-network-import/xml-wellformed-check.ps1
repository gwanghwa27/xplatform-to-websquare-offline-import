# Helper for BUILD-AND-VERIFY.cmd. NOT part of the Production converter.
# Validates every *.xml under -OutputRoot parses as well-formed XML using .NET's own
# parser (System.Xml, built into PowerShell -- no external dependency).
#
# Usage: powershell -NoProfile -File xml-wellformed-check.ps1 -OutputRoot "<path>"
# Output: XML_PARSE_TOTAL=N / XML_PARSE_ERR=N, plus one FAIL line per bad file.
# Exit code 0 (all well-formed) / 1 (one or more parse errors).

param(
    [Parameter(Mandatory = $true)]
    [string]$OutputRoot
)

$files = Get-ChildItem -Path $OutputRoot -Filter *.xml -Recurse -File
$err = 0
foreach ($f in $files) {
    try {
        $doc = New-Object System.Xml.XmlDocument
        $doc.Load($f.FullName)
    } catch {
        Write-Output ("FAIL: " + $f.FullName + " -- " + $_.Exception.Message)
        $err++
    }
}
Write-Output ("XML_PARSE_TOTAL=" + $files.Count)
Write-Output ("XML_PARSE_ERR=" + $err)

if ($err -gt 0) { exit 1 } else { exit 0 }
