param(
    [string]$BuildType = "debug"
)

$ProjectRoot = "C:\Users\John Victor\Documents\Development\AttentionPanner"
$PackageName = "com.jv.attentionpanner"
$ActivityName = "$PackageName.MainActivity"

Write-Host "=== Building $BuildType APK ===" -ForegroundColor Cyan
& "$ProjectRoot\gradlew.bat" "assemble$($BuildType.Substring(0,1).ToUpper() + $BuildType.Substring(1))" -p $ProjectRoot
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`n=== Uninstalling old version ===" -ForegroundColor Cyan
adb uninstall $PackageName 2>$null

Write-Host "`n=== Installing new version ===" -ForegroundColor Cyan
$ApkPath = Get-ChildItem -Path "$ProjectRoot\app\build\outputs\apk\$BuildType" -Filter "*universal*$BuildType.apk" | Select-Object -First 1
if (-not $ApkPath) {
    $ApkPath = Get-ChildItem -Path "$ProjectRoot\app\build\outputs\apk\$BuildType" -Filter "*$BuildType.apk" | Select-Object -First 1
}
if (-not $ApkPath) {
    Write-Host "APK not found!" -ForegroundColor Red
    exit 1
}
adb install "$($ApkPath.FullName)"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`n=== Launching app ===" -ForegroundColor Cyan
adb shell am start -n "$PackageName/$ActivityName"

Write-Host "`nDone!" -ForegroundColor Green
