param(
  [string]$OutputDirectory = "$PSScriptRoot\release"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

# 1. Recreate the uploadable release directory.
if (Test-Path -LiteralPath $OutputDirectory) {
  Remove-Item -LiteralPath $OutputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
New-Item -ItemType Directory -Path "$OutputDirectory\pc" | Out-Null
New-Item -ItemType Directory -Path "$OutputDirectory\h5" | Out-Null
New-Item -ItemType Directory -Path "$OutputDirectory\backend" | Out-Null
New-Item -ItemType Directory -Path "$OutputDirectory\tools" | Out-Null
New-Item -ItemType Directory -Path "$OutputDirectory\tools\mysql" | Out-Null
New-Item -ItemType Directory -Path "$OutputDirectory\tools\nginx" | Out-Null

# 2. Build PC/H5 static assets and the backend JAR locally.
Push-Location "$projectRoot\for-next-time-pc"
npm run build
Pop-Location

Push-Location "$projectRoot\for-next-time-h5"
npm run build
Pop-Location

Push-Location "$projectRoot\for-next-time-server"
cmd /c mvn.cmd -q -DskipTests package
Pop-Location

# 3. Copy only runtime files into the uploadable release directory.
Copy-Item -Path "$projectRoot\for-next-time-pc\dist\*" -Destination "$OutputDirectory\pc" -Recurse
Copy-Item -Path "$projectRoot\for-next-time-h5\dist\*" -Destination "$OutputDirectory\h5" -Recurse
$jar = Get-ChildItem -Path "$projectRoot\for-next-time-server\target\for-next-time-server-*.jar" | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
if ($null -eq $jar) {
  throw 'Backend JAR was not created.'
}
Copy-Item -LiteralPath $jar.FullName -Destination "$OutputDirectory\backend\app.jar"
Copy-Item -LiteralPath "$PSScriptRoot\start.sh" -Destination "$OutputDirectory\start.sh"
Copy-Item -LiteralPath "$PSScriptRoot\.env.example" -Destination "$OutputDirectory\.env.example"
Copy-Item -LiteralPath "$PSScriptRoot\backend\app.sh" -Destination "$OutputDirectory\backend\app.sh"
Copy-Item -LiteralPath "$PSScriptRoot\tools\mysql\start.sh" -Destination "$OutputDirectory\tools\mysql\start.sh"
Copy-Item -LiteralPath "$PSScriptRoot\tools\nginx\for-next-time.server.conf.template" -Destination "$OutputDirectory\tools\nginx\for-next-time.server.conf.template"

Write-Host "Release package created: $OutputDirectory"
Write-Host "Upload the files inside release to /data/for-next-time/"
