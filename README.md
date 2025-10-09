# Mastermind (React + Vite)

A simple Mastermind game. The secret is a sequence of 4 colors from a palette of 6. Pick colors for each slot and submit your guess. The history lists your previous attempts with feedback:

- Exact: right color in the right position
- Partial: right color in the wrong position

Right-click a slot to clear it. Use “New Game” to generate a new secret.

## Run locally

1. Install dependencies
2. Start the dev server

```pwsh
npm install
npm run dev
```

Then open the printed local URL in your browser.

## Build

```pwsh
npm run build
npm run preview
```

## Tech

- React 18
- Vite 5
- TypeScript 5

## Notes

- The solution can include repeated colors.
- For accessibility: buttons have titles; keyboard users can select slots then choose colors.

## Azure Deployment

This project includes scripts to deploy to Azure Static Web Apps.

### Prerequisites

- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- Azure subscription with a Static Web App named "giac-static-app" in resource group "giac-static-app_group"
  (modify the script if your resource names differ)

### Deploy to Azure

#### Option 1: Using PowerShell

```pwsh
# Login to Azure (if not already logged in)
az login

# Run the deployment script
./deploy-to-azure.ps1
```

#### Option 2: Using Batch File

Simply double-click the `deploy-to-azure.bat` file in Windows Explorer.

### Deployment Script Details

The deployment script:

1. Checks for prerequisites (Azure CLI and SWA CLI)
2. Builds the application with `npm run build`
3. Ensures configuration files are in place
4. Gets the deployment token from Azure
5. Deploys to Azure Static Web App
6. Displays the URL where your app is available

After successful deployment, your Mastermind game will be available at:
https://nice-sand-04c84f41e.1.azurestaticapps.net (or your custom domain if configured)
