<#
.SYNOPSIS
    Pre-commit checks: the static Plugin Hub and rules checks, then the Gradle build.

.DESCRIPTION
    Runs scripts/Check.java (Plugin Hub requirements and the rules in AGENTS.md / docs/rules.md that
    can be found by reading files), then .\gradlew.bat build. Both parts always run, so one run
    reports everything. Exits non-zero if either fails. Mirrors scripts/check.sh.

        powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1            # static checks + build
        powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -NoBuild   # static checks only
        powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Submit    # also fail on placeholder names

    Also available in VS Code as Terminal > Run Task > "check".
#>
param(
    [switch]$NoBuild,
    [switch]$Submit
)

$ErrorActionPreference = 'Stop'

# Same JDK the Gradle wrapper uses; fall back to whatever 'java' is on PATH.
$java = 'java'
if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME 'bin\java.exe' }

$checkArgs = @()
if ($Submit) { $checkArgs += '--submit' }

Push-Location (Join-Path $PSScriptRoot '..')
try
{
    $failed = $false

    & $java scripts/Check.java @checkArgs
    if ($LASTEXITCODE -ne 0) { $failed = $true }

    if (-not $NoBuild)
    {
        Write-Host "`n==> .\gradlew.bat build"
        & .\gradlew.bat build --console=plain
        if ($LASTEXITCODE -ne 0) { $failed = $true }
    }
}
finally
{
    Pop-Location
}

if ($failed) { exit 1 }
