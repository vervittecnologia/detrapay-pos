param(
    [string]$Serial = $env:ANDROID_SERIAL
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$appApk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
$testApk = Join-Path $projectRoot "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
$deviceArgs = if ([string]::IsNullOrWhiteSpace($Serial)) { @() } else { @("-s", $Serial) }
$appInstalled = $false
$testFailed = $false

function Invoke-Adb {
    param([string[]]$CommandArgs)

    & adb @deviceArgs @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb falhou: $($CommandArgs -join ' ')"
    }
}

Push-Location $projectRoot
try {
    & .\gradlew.bat --offline assembleDebug assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao gerar os APKs de app e teste."
    }

    Invoke-Adb @("logcat", "-c")
    Invoke-Adb @("install", "-r", $appApk)
    $appInstalled = $true
    Invoke-Adb @("install", "-r", "-t", $testApk)

    $instrumentOutput = & adb @deviceArgs shell am instrument -w -r `
        "com.detrapay.test/com.detrapay.testing.DetrapayTestRunner" 2>&1
    $instrumentExitCode = $LASTEXITCODE
    $instrumentOutput | Write-Output
    $joinedOutput = $instrumentOutput -join "`n"
    $testFailed = $instrumentExitCode -ne 0 -or $joinedOutput -match "(?m)^FAILURES!!!"
}
finally {
    if ($appInstalled) {
        & adb @deviceArgs shell am start -n com.detrapay/.ui.splash.SplashActivity
        & adb @deviceArgs logcat -d |
            Select-String -Pattern "com.detrapay|AndroidRuntime|FATAL EXCEPTION"
    }
    Pop-Location
}

if ($testFailed) {
    throw "Os testes instrumentados falharam."
}
