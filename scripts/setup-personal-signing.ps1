# Run locally on Windows with JDK 17+ and GitHub CLI installed.
# Creates a NEW key outside the checkout, backs its password up with Windows DPAPI,
# and uploads signing material directly to repository Actions secrets via stdin.
$ErrorActionPreference = 'Stop'
$repository = 'lorenzomp7/OpenVitals'
Get-Command gh, keytool -ErrorAction Stop | Out-Null
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'Run gh auth login first.' }
$signingDirectory = Join-Path $env:LOCALAPPDATA 'OpenVitalsSigning'
New-Item -ItemType Directory -Force -Path $signingDirectory | Out-Null
$keyPath = Join-Path $signingDirectory 'personal-chatgpt.p12'
$passwordPath = Join-Path $signingDirectory 'personal-chatgpt.password.dpapi'
if ((Test-Path $keyPath) -ne (Test-Path $passwordPath)) {
    throw 'Incomplete signing backup. Restore the matching key and password files before continuing.'
}
if (-not (Test-Path $keyPath)) {
    $random = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($random) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($random)
    $env:OPENVITALS_LOCAL_KEY_PASSWORD = $password
    try {
        & keytool -genkeypair -storetype PKCS12 -keystore $keyPath -alias personal `
            -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=OpenVitals Personal' `
            -storepass:env OPENVITALS_LOCAL_KEY_PASSWORD -keypass:env OPENVITALS_LOCAL_KEY_PASSWORD
        if ($LASTEXITCODE -ne 0) { throw 'Could not generate signing key.' }
        ConvertTo-SecureString $password -AsPlainText -Force | ConvertFrom-SecureString | Set-Content $passwordPath
    } finally { Remove-Item Env:OPENVITALS_LOCAL_KEY_PASSWORD -ErrorAction SilentlyContinue }
} else {
    $securePassword = Get-Content $passwordPath | ConvertTo-SecureString
    $credential = New-Object System.Net.NetworkCredential('', $securePassword)
    $password = $credential.Password
}
function Set-RepositorySecret([string]$name, [string]$value) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = (Get-Command gh).Source
    $info.Arguments = "secret set $name --repo $repository"
    $info.UseShellExecute = $false
    $info.RedirectStandardInput = $true
    $process = [System.Diagnostics.Process]::Start($info)
    try {
        # Write exactly the secret, without the newline added by a PowerShell pipeline.
        $process.StandardInput.Write($value)
        $process.StandardInput.Close()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "Could not configure $name. Existing local key remains available." }
    } finally { $process.Dispose() }
}
try {
    Set-RepositorySecret 'OPENVITALS_PERSONAL_KEYSTORE_BASE64' ([Convert]::ToBase64String([IO.File]::ReadAllBytes($keyPath)))
    Set-RepositorySecret 'OPENVITALS_PERSONAL_STORE_PASSWORD' $password
    Set-RepositorySecret 'OPENVITALS_PERSONAL_KEY_ALIAS' 'personal'
    Write-Host "Signing secrets configured. Keep the backup in $signingDirectory. The password backup is protected for this Windows account."
} finally { $password = $null; $credential = $null; $securePassword = $null }
