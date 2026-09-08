param (
    [int]$Port = 0
)

# 1. Detection de cloudflared.exe
$docPath = "$HOME\Documents\cloudflared\cloudflared.exe"

if (Get-Command "cloudflared" -ErrorAction SilentlyContinue) {
    $cloudflaredCmd = "cloudflared"
} elseif (Test-Path ".\cloudflared.exe") {
    $cloudflaredCmd = ".\cloudflared.exe"
} elseif (Test-Path $docPath) {
    $cloudflaredCmd = $docPath
} else {
    Write-Host "[ERR] Impossible de trouver cloudflared.exe." -ForegroundColor Red
    exit 1
}

# 2. Port dans application.yml
if ($Port -eq 0) {
    $ymlFile = "application.yml"
    if (Test-Path $ymlFile) {
        $match = Select-String -Path $ymlFile -Pattern "port:\s*(\d+)"
        if ($match) {
            $Port = $match.Matches.Groups[1].Value
        }
    }
    if ($Port -eq 0) { $Port = 8086 }
}

Write-Host "[+] Port configure : $Port" -ForegroundColor Yellow
Write-Host "[+] Demarrage du tunnel Cloudflare..." -ForegroundColor Cyan

# Nettoyage de l'ancien fichier de log
if (Test-Path "cloudflare.log") { Remove-Item "cloudflare.log" -Force }

# 3. Lancement du processus Cloudflare
try {
    $cloudflaredProcess = Start-Process -FilePath $cloudflaredCmd -ArgumentList "tunnel --url http://localhost:$Port" -RedirectStandardError "cloudflare.log" -PassThru -NoNewWindow
} catch {
    Write-Host "[ERR] Echec du demarrage de cloudflared." -ForegroundColor Red
    exit 1
}

# 4. Attente active de l'URL (jusqu'a 15 secondes)
$TUNNEL_URL = $null
$timeoutSeconds = 15
$elapsed = 0

Write-Host "[+] Generation de l'URL en cours" -NoNewline
while ($elapsed -lt $timeoutSeconds) {
    Start-Sleep -Seconds 1
    $elapsed++
    Write-Host "." -NoNewline -ForegroundColor Gray

    if (Test-Path "cloudflare.log") {
        $logContent = Get-Content "cloudflare.log" -Raw -ErrorAction SilentlyContinue
        if ($logContent) {
            $matchUrl = [regex]::Match($logContent, 'https://[-a-zA-Z0-9]+\.trycloudflare\.com')
            if ($matchUrl.Success) {
                $TUNNEL_URL = $matchUrl.Value
                break
            }
        }
    }
}
Write-Host "" # Saut de ligne

# 5. Démarrage de Spring Boot
if ($TUNNEL_URL) {
    Write-Host "[OK] URL Cloudflare detectee : $TUNNEL_URL" -ForegroundColor Green
    Write-Host "[+] Demarrage de l'application Spring Boot sur le port $Port..." -ForegroundColor Cyan
    $env:APP_BASE_URL = $TUNNEL_URL
    .\mvnw.cmd spring-boot:run
} else {
    Write-Host "[ERR] Delai depasse. Impossible de recuperer l'URL dans cloudflare.log." -ForegroundColor Red
    Write-Host "--- Contenu actuel du log ---" -ForegroundColor Yellow
    Get-Content "cloudflare.log" -ErrorAction SilentlyContinue
    if ($cloudflaredProcess -and $cloudflaredProcess.Id) {
        Stop-Process -Id $cloudflaredProcess.Id -ErrorAction SilentlyContinue
    }
}