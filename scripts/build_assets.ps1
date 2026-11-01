<#
.SYNOPSIS
  重新编译 assets/adb-switch 与 assets/simple-home 两个 APK。

.DESCRIPTION
  需要：Windows PowerShell 5.1+ / PowerShell 7、JDK 17-22（含 javac/jar）、
  Android SDK（build-tools 34+、platforms/android-34 或 35）。
  JDK 默认取 $env:JAVA_HOME；未设置时尝试 Android Studio 自带 JBR。
  Android SDK 默认取 $env:ANDROID_HOME / $env:ANDROID_SDK_ROOT，
  否则尝试 %LOCALAPPDATA%\Android\Sdk。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\build_assets.ps1
  powershell -ExecutionPolicy Bypass -File scripts\build_assets.ps1 -OutDir .\dist
#>
param(
    [string]$AndroidSdk = "",
    [string]$Jdk = "",
    [string]$OutDir = "",
    [int]$MinSdk = 16
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
if (-not $OutDir) { $OutDir = Join-Path $root "dist" }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Find-Sdk {
    if ($AndroidSdk) { return $AndroidSdk }
    foreach ($p in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
                     (Join-Path $env:LOCALAPPDATA "Android\Sdk"),
                     (Join-Path $env:USERPROFILE "Android\Sdk"))) {
        if ($p -and (Test-Path (Join-Path $p "build-tools"))) { return $p }
    }
    throw "找不到 Android SDK：请设置 -AndroidSdk 或 ANDROID_HOME"
}

function Find-Jdk {
    if ($Jdk) { return $Jdk }
    if ($env:JAVA_HOME) { return $env:JAVA_HOME }
    foreach ($p in @("C:\Program Files\Android\Android Studio\jbr",
                     "C:\Program Files\Java\jdk-17",
                     "C:\Program Files\Eclipse Adoptium\jdk-17*")) {
        $hit = Get-Item $p -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    throw "找不到 JDK：请设置 -Jdk 或 JAVA_HOME（需要 JDK 17-22）"
}

$sdk = Find-Sdk
$jdk = Find-Jdk
$bt = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
      Sort-Object { [int]($_.Name.Split('.')[0]) } -Descending | Select-Object -First 1
if (-not $bt) { throw "找不到 build-tools" }
$btDir = $bt.FullName
$platforms = Get-ChildItem (Join-Path $sdk "platforms") -Directory -ErrorAction SilentlyContinue
if (-not $platforms) { throw "找不到 platforms/android-*" }
$androidJar = Join-Path $platforms[-1].FullName "android.jar"

$env:JAVA_HOME = $jdk
$env:PATH = (Join-Path $jdk "bin") + ";" + $btDir + ";" + $env:PATH
$ks = Join-Path $OutDir "debug.keystore"
if (-not (Test-Path $ks)) {
    & (Join-Path $jdk "bin\keytool.exe") -genkeypair -v -keystore $ks `
        -storepass android -keypass android -alias androiddebugkey `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US" | Out-Null
}

function Build-Apk([string]$srcDir, [string]$pkgPath, [string]$outName) {
    Write-Host "== building $outName =="
    Push-Location $srcDir
    try {
        Remove-Item -Recurse -Force classes, dexout, gen, compiled_res.zip, classes.jar,
            app.unsigned.apk, app.withdex.apk, app.aligned.apk -ErrorAction SilentlyContinue
        & (Join-Path $btDir "aapt2.exe") compile --dir res -o compiled_res.zip
        & (Join-Path $btDir "aapt2.exe") link -o app.unsigned.apk -I $androidJar `
            --manifest AndroidManifest.xml -R compiled_res.zip --java gen `
            --min-sdk-version $MinSdk --target-sdk-version 18 --auto-add-overlay
        New-Item -ItemType Directory -Force -Path classes | Out-Null
        $java = @("src\$pkgPath\MainActivity.java")
        if (Test-Path "gen\$pkgPath\R.java") { $java += "gen\$pkgPath\R.java" }
        & (Join-Path $jdk "bin\javac.exe") -source 8 -target 8 -bootclasspath $androidJar `
            -d classes @java
        & (Join-Path $jdk "bin\jar.exe") cf classes.jar -C classes .
        New-Item -ItemType Directory -Force -Path dexout | Out-Null
        $d8 = Join-Path $btDir "d8.bat"
        if (-not (Test-Path $d8)) { $d8 = Join-Path $btDir "d8" }
        & $d8 --lib $androidJar --min-api $MinSdk --output dexout classes.jar

        $py = (Get-Command python -ErrorAction SilentlyContinue).Source
        if (-not $py) { throw "需要 Python 3 来把 classes.dex 打进 APK" }
        & $py -c @"
import zipfile
src=r'$srcDir\app.unsigned.apk'; out=r'$srcDir\app.withdex.apk'
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist(): zout.writestr(item, zin.read(item.filename))
    zout.write(r'$srcDir\dexout\classes.dex','classes.dex')
"@
        & (Join-Path $btDir "zipalign.exe") -f 4 app.withdex.apk app.aligned.apk
        & (Join-Path $btDir "apksigner.bat") sign --ks $ks --ks-pass pass:android `
            --key-pass pass:android --ks-key-alias androiddebugkey `
            --out (Join-Path $OutDir $outName) app.aligned.apk
        Write-Host "built: $(Join-Path $OutDir $outName)"
    } finally {
        Pop-Location
    }
}

Build-Apk (Join-Path $root "assets\adb-switch") "com\dsh\adbhelper" "ADB-Switch.apk"
Build-Apk (Join-Path $root "assets\simple-home") "com\dsh\simplehome" "SimpleHome.apk"
Write-Host "全部完成，输出目录: $OutDir"
