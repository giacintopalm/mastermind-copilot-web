# deploy-to-azure.ps1
# Script to deploy Mastermind game to Azure Static Web App
# Created on October 8, 2025

# Configuration - Update these values if needed
$resourceGroupName = "giac-static-app_group"
$staticWebAppName = "giac-static-app"
$buildFolder = "dist"

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "  Mastermind Game - Azure Static Web App Deployment" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan

# Step 1: Check prerequisites
Write-Host "`n[1/5] Checking prerequisites..." -ForegroundColor Green

# Check if Azure CLI is installed
try {
    $azVersion = az --version
    Write-Host "✓ Azure CLI is installed" -ForegroundColor Green
}
catch {
    Write-Host "✗ Azure CLI is not installed. Please install it from: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli" -ForegroundColor Red
    exit 1
}

# Check if Static Web Apps CLI is installed
try {
    $swaVersion = swa --version
    Write-Host "✓ Static Web Apps CLI is installed" -ForegroundColor Green
}
catch {
    Write-Host "! Static Web Apps CLI is not installed. Installing now..." -ForegroundColor Yellow
    npm install -g @azure/static-web-apps-cli
}

# Step 2: Build the application
Write-Host "`n[2/5] Building the Mastermind application..." -ForegroundColor Green
npm run build

if (-not (Test-Path -Path $buildFolder)) {
    Write-Host "✗ Build failed - $buildFolder folder not found" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Build completed successfully" -ForegroundColor Green

# Step 3: Ensure staticwebapp.config.json is in dist folder
Write-Host "`n[3/5] Copying configuration files..." -ForegroundColor Green

if (Test-Path -Path "staticwebapp.config.json") {
    Copy-Item -Path "staticwebapp.config.json" -Destination "$buildFolder/staticwebapp.config.json" -Force
    Write-Host "✓ Configuration file copied to $buildFolder" -ForegroundColor Green
} else {
    Write-Host "! staticwebapp.config.json not found, creating default configuration..." -ForegroundColor Yellow
    
    # Create a default configuration
    @"
{
  "navigationFallback": {
    "rewrite": "/index.html",
    "exclude": ["/assets/*.{js,css,png,jpg,gif}", "/.well-known/*"]
  },
  "routes": [
    {
      "route": "/assets/*",
      "headers": {
        "cache-control": "must-revalidate, max-age=15770000"
      }
    },
    {
      "route": "/*",
      "rewrite": "/index.html"
    }
  ],
  "platform": {
    "apiRuntime": "node:20"
  }
}
"@ | Set-Content -Path "$buildFolder/staticwebapp.config.json"
    
    Write-Host "✓ Default configuration created in $buildFolder" -ForegroundColor Green
}

# Step 4: Get deployment token
Write-Host "`n[4/5] Getting deployment token..." -ForegroundColor Green

try {
    $deployToken = az staticwebapp secrets list --name $staticWebAppName --resource-group $resourceGroupName --query properties.apiKey -o tsv
    
    if ([string]::IsNullOrEmpty($deployToken)) {
        throw "Empty deployment token"
    }
    
    # Save token to temporary file to avoid command line length issues
    $deployToken | Out-File -FilePath "deploy-token.txt"
    Write-Host "✓ Deployment token retrieved successfully" -ForegroundColor Green
}
catch {
    Write-Host "✗ Failed to get deployment token. Are you logged in to Azure? Run 'az login' first." -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
    exit 1
}

# Step 5: Deploy to Azure
Write-Host "`n[5/5] Deploying to Azure Static Web App ($staticWebAppName)..." -ForegroundColor Green

try {
    swa deploy "./$buildFolder" --deployment-token (Get-Content deploy-token.txt) --env production
    
    # Get the URL of the deployed site
    $siteUrl = az staticwebapp show --name $staticWebAppName --resource-group $resourceGroupName --query "defaultHostname" -o tsv
    
    # Clean up the token file
    Remove-Item -Path "deploy-token.txt" -Force
    
    Write-Host "`n===============================================" -ForegroundColor Cyan
    Write-Host "  Deployment Completed Successfully!" -ForegroundColor Green
    Write-Host "  Your Mastermind game is now available at:" -ForegroundColor Cyan
    Write-Host "  https://$siteUrl" -ForegroundColor Yellow
    Write-Host "===============================================" -ForegroundColor Cyan
}
catch {
    Write-Host "✗ Deployment failed" -ForegroundColor Red
    Write-Host "  Error: $_" -ForegroundColor Red
    
    # Clean up the token file even if deployment fails
    if (Test-Path -Path "deploy-token.txt") {
        Remove-Item -Path "deploy-token.txt" -Force
    }
    
    exit 1
}