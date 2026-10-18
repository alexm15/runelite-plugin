<#
.SYNOPSIS
    Sets up a Windows dev environment for this RuneLite plugin.

.DESCRIPTION
    - Installs (via winget, skipping anything already installed):
        EclipseAdoptium.Temurin.11.JDK, Git.Git, Microsoft.VisualStudioCode, GitHub.cli
    - Installs the VS Code extensions vscjava.vscode-java-pack and vscjava.vscode-gradle
    - Sets machine-level JAVA_HOME to the newest Temurin JDK 11 under C:\Program Files\Eclipse Adoptium

    Gradle is NOT installed: the project uses the Gradle wrapper (gradlew.bat), which downloads
    the right Gradle version on first use.

    Run from an elevated PowerShell (needed for the machine-level JAVA_HOME):
        powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev.ps1

    Safe to re-run: every step checks first and skips what is already done.
#>

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Message) { Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Write-Ok([string]$Message) { Write-Host "    $Message" -ForegroundColor Green }
function Write-Skip([string]$Message) { Write-Host "    $Message" -ForegroundColor DarkGray }
function Write-Warn([string]$Message) { Write-Host "    WARNING: $Message" -ForegroundColor Yellow }

# Re-read PATH from the registry so tools installed by this script are usable without a new terminal.
function Update-SessionPath
{
    $machine = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $user = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = "$machine;$user"
}

# --- Preconditions -----------------------------------------------------------

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin)
{
    Write-Host 'This script must run as Administrator (it sets JAVA_HOME at machine level).' -ForegroundColor Red
    Write-Host 'Right-click PowerShell or Windows Terminal > "Run as administrator", then run:' -ForegroundColor Red
    Write-Host '    powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev.ps1' -ForegroundColor Red
    exit 1
}

if (-not (Get-Command winget -ErrorAction SilentlyContinue))
{
    Write-Host 'winget was not found. Install or update "App Installer" from the Microsoft Store, then re-run.' -ForegroundColor Red
    exit 1
}

# --- 1. winget packages ------------------------------------------------------

$packages = @(
    'EclipseAdoptium.Temurin.11.JDK',
    'Git.Git',
    'Microsoft.VisualStudioCode',
    'GitHub.cli'
)

Write-Step 'Installing tools with winget'
foreach ($id in $packages)
{
    # 'winget list' exits non-zero when no installed package matches the id.
    winget list --id $id --exact --accept-source-agreements | Out-Null
    if ($LASTEXITCODE -eq 0)
    {
        Write-Skip "$id is already installed, skipping"
        continue
    }

    Write-Host "    Installing $id ..."
    winget install --id $id --exact --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0)
    {
        Write-Warn "winget could not install $id (exit code $LASTEXITCODE). Install it manually and re-run."
    }
    else
    {
        Write-Ok "$id installed"
    }
}

Update-SessionPath

# --- 2. VS Code extensions ---------------------------------------------------

$extensions = @(
    'vscjava.vscode-java-pack',
    'vscjava.vscode-gradle'
)

Write-Step 'Installing VS Code extensions'
$code = Get-Command code -ErrorAction SilentlyContinue
if (-not $code)
{
    # Fallbacks for when PATH has not picked up VS Code yet (user and machine installs).
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\Microsoft VS Code\bin\code.cmd",
        "$env:ProgramFiles\Microsoft VS Code\bin\code.cmd"
    )
    $found = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($found) { $code = Get-Command $found }
}

if (-not $code)
{
    Write-Warn 'The VS Code "code" command was not found. Open a new terminal and re-run this script,'
    Write-Warn 'or install the extensions from the Extensions view (they are recommended in .vscode/extensions.json).'
}
else
{
    $installed = & $code --list-extensions
    foreach ($ext in $extensions)
    {
        if ($installed -contains $ext)
        {
            Write-Skip "$ext is already installed, skipping"
            continue
        }
        & $code --install-extension $ext
        if ($LASTEXITCODE -ne 0) { Write-Warn "Could not install VS Code extension $ext" }
        else { Write-Ok "$ext installed" }
    }
}

# --- 3. JAVA_HOME ------------------------------------------------------------

Write-Step 'Setting JAVA_HOME'
$adoptiumRoot = 'C:\Program Files\Eclipse Adoptium'

# Folder names look like 'jdk-11.0.24.8-hotspot'; sort on the parsed version, not the string.
$jdk = $null
if (Test-Path $adoptiumRoot)
{
    $jdk = Get-ChildItem -Path $adoptiumRoot -Directory -Filter 'jdk-11*' |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
        Sort-Object -Descending -Property @{ Expression = {
            $v = $_.Name -replace '^jdk-', '' -replace '-hotspot$', ''
            try { [version]$v } catch { [version]'0.0' }
        } } |
        Select-Object -First 1
}

if (-not $jdk)
{
    Write-Warn "No Temurin JDK 11 found under '$adoptiumRoot'. JAVA_HOME was NOT changed."
    Write-Warn 'Check that EclipseAdoptium.Temurin.11.JDK installed correctly (winget list --id EclipseAdoptium.Temurin.11.JDK).'
}
else
{
    $current = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    if ($current -eq $jdk.FullName)
    {
        Write-Skip "JAVA_HOME is already $($jdk.FullName)"
    }
    else
    {
        [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk.FullName, 'Machine')
        Write-Ok "JAVA_HOME (machine) = $($jdk.FullName)"
    }

    # The Temurin installer normally adds its bin folder to PATH. Warn if another Java would win.
    $jdkBin = Join-Path $jdk.FullName 'bin'
    $firstJava = (Get-Command java -ErrorAction SilentlyContinue | Select-Object -First 1).Source
    if (-not $firstJava)
    {
        Write-Warn "'java' is not on PATH. Add $jdkBin to your PATH, or Gradle will still work via JAVA_HOME."
    }
    elseif (-not $firstJava.StartsWith($jdkBin, [StringComparison]::OrdinalIgnoreCase))
    {
        Write-Warn "'java' on PATH resolves to $firstJava, not JDK 11."
        Write-Warn 'The Gradle wrapper uses JAVA_HOME, so builds are fine, but "java -version" will show that other JDK.'
    }
}

# --- Done --------------------------------------------------------------------

Write-Step 'Done'
Write-Host '    Open a NEW terminal (so it picks up JAVA_HOME and PATH), then verify with:'
Write-Host '        java -version          # should report 11.x (Temurin)'
Write-Host '        .\gradlew.bat --version'
Write-Host '        .\gradlew.bat run      # launches RuneLite with the plugin in developer mode'
