function Package-javTex {
    $ErrorActionPreference = "Stop"

    $ProjectDir = $PSScriptRoot
    [xml]$Pom = Get-Content (Join-Path $ProjectDir "pom.xml")
    $VersionNode = $Pom.SelectSingleNode(
        "/*[local-name()='project']/*[local-name()='properties']/*[local-name()='javtex.version']"
    )
    if ($null -eq $VersionNode -or [string]::IsNullOrWhiteSpace($VersionNode.InnerText)) {
        throw "Could not find javtex.version in pom.xml."
    }

    $AppName = "javTex"
    $AppVersion = $VersionNode.InnerText.Trim()
    $JarName = "javTex-$AppVersion.jar"
    $JPackageCandidates = @()
    $JPackageCommand = Get-Command "jpackage.exe" -ErrorAction SilentlyContinue
    if ($null -ne $JPackageCommand) {
        $JPackageCandidates += $JPackageCommand.Source
    }
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $JPackageCandidates += Join-Path $env:JAVA_HOME "bin\jpackage.exe"
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ProgramFiles)) {
        $JavaInstallRoot = Join-Path $env:ProgramFiles "Java"
        if (Test-Path $JavaInstallRoot) {
            $JPackageCandidates += Get-ChildItem $JavaInstallRoot -Directory |
                Sort-Object Name -Descending |
                ForEach-Object { Join-Path $_.FullName "bin\jpackage.exe" }
        }
    }

    $JPackage = $JPackageCandidates |
        Where-Object { $_ -and (Test-Path $_) } |
        Select-Object -First 1
    if ($null -eq $JPackage) {
        throw "Could not find jpackage.exe. Install a JDK or set JAVA_HOME to its installation directory."
    }

    $Icon = Join-Path $ProjectDir "src\main\resources\icon.ico"
    $Target = Join-Path $ProjectDir "target"
    $Dist = Join-Path $ProjectDir "dist"

    # Check required files
    if (!(Test-Path $Icon)) {
        throw "Could not find application icon: $Icon"
    }

    # Build the application
    Write-Host "Building javTex..." -ForegroundColor Cyan
    if ($null -eq (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
        throw "Could not find Maven (mvn). Install Maven and add it to PATH."
    }

    Push-Location $ProjectDir
    try {
        & mvn clean package
        $MavenExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($MavenExitCode -ne 0) {
        throw "Maven build failed. Installer was not created"
    }

    # Prepare output directory
    New-Item -ItemType Directory -Path $Dist -Force | Out-Null

    # Remove old installer with the same name
    $Installer = Join-Path $Dist "$AppName-$AppVersion.exe"
    if (Test-Path $Installer) {
        Remove-Item $Installer -Force
    }

    # Package the application
    Write-Host "Creating Windows installer..." -ForegroundColor Cyan

    & $JPackage `
        --type exe `
        --name $AppName `
        --app-version $AppVersion `
        --vendor "Ben Barton-Foskett" `
        --description "A Java-based LaTeX editor with integrated PDF viewer" `
        --copyright "Copyright (c) 2026 Ben Barton-Foskett" `
        --input $Target `
        --main-jar $JarName `
        --main-class "com.javtex.latex.Main" `
        --dest $Dist `
        --icon $Icon `
        --win-menu `
        --win-shortcut `
        --win-dir-chooser

    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }

    if (!(Test-Path $Installer)) {
        throw "jpackage completed but the expected installer was not found: $Installer"
    }

    Write-Host "Installer created: $Installer" -ForegroundColor Green
}

Package-javTex
