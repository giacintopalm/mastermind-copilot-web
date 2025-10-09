# Mastermind Game - Java Backend + React Frontend# Mastermind (React + Vite)



A modern implementation of the classic Mastermind game with a Java Spring Boot backend and React frontend, designed for deployment on Azure.A simple Mastermind game. The secret is a sequence of 4 colors from a palette of 6. Pick colors for each slot and submit your guess. The history lists your previous attempts with feedback:



## Architecture- Exact: right color in the right position

- Partial: right color in the wrong position

This project uses a **separated backend/frontend architecture**:

Right-click a slot to clear it. Use “New Game” to generate a new secret.

- **Backend** (`/backend`): Java 21 Spring Boot REST API

- **Frontend** (`/frontend`): React + TypeScript + Vite SPA## Run locally

- **Infrastructure** (`/infra`): Azure Bicep templates for deployment

1. Install dependencies

## Project Structure2. Start the dev server



``````pwsh

mastermind-copilot-java/npm install

├── backend/                    # Java Spring Boot APInpm run dev

│   ├── src/main/java/com/mastermind/```

│   │   ├── controller/         # REST API controllers

│   │   ├── service/           # Business logic servicesThen open the printed local URL in your browser.

│   │   ├── model/             # Data models

│   │   ├── config/            # Configuration classes## Build

│   │   └── MastermindApplication.java

│   ├── src/main/resources/```pwsh

│   │   └── application.propertiesnpm run build

│   ├── pom.xml                # Maven configurationnpm run preview

│   └── mvnw / mvnw.cmd        # Maven wrapper```

├── frontend/                   # React frontend

│   ├── src/## Tech

│   │   ├── App.tsx            # Main React component

│   │   ├── api.ts             # API service layer- React 18

│   │   ├── main.tsx           # React entry point- Vite 5

│   │   └── styles.css         # Styles- TypeScript 5

│   ├── package.json           # npm configuration

│   ├── vite.config.ts         # Vite configuration## Notes

│   └── tsconfig.json          # TypeScript configuration

├── infra/                      # Azure infrastructure- The solution can include repeated colors.

│   ├── main.bicep             # Main Bicep template- For accessibility: buttons have titles; keyboard users can select slots then choose colors.

│   ├── main.parameters.json   # Template parameters

│   └── core/                  # Bicep modules## Azure Deployment

├── .azure/                     # Azure deployment hooks

└── azure.yaml                 # Azure Developer CLI configThis project includes scripts to deploy to Azure Static Web Apps.

```

### Prerequisites

## Game Logic

- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)

The game implements standard Mastermind rules:- Azure subscription with a Static Web App named "giac-static-app" in resource group "giac-static-app_group"

- 4-slot secret code using 6 colors (red, blue, green, yellow, purple, cyan)  (modify the script if your resource names differ)

- Players guess the secret code

- Feedback shows exact matches (correct color + position) and partial matches (correct color, wrong position)### Deploy to Azure

- Game ends when all slots are guessed correctly

#### Option 1: Using PowerShell

### Backend API Endpoints

```pwsh

- `POST /api/games` - Create new game# Login to Azure (if not already logged in)

- `GET /api/games/{id}` - Get game stateaz login

- `POST /api/games/{id}/guesses` - Submit guess

- `GET /api/games/{id}/solution` - Get solution (spoiler)# Run the deployment script

- `POST /api/games/{id}/reset` - Reset game./deploy-to-azure.ps1

- `GET /api/games/colors` - Get available colors```



## Development#### Option 2: Using Batch File



### PrerequisitesSimply double-click the `deploy-to-azure.bat` file in Windows Explorer.



- **Java 21** (for backend)### Deployment Script Details

- **Node.js 18+** (for frontend)

- **Azure CLI** (for deployment)The deployment script:

- **Azure Developer CLI** (for deployment)

1. Checks for prerequisites (Azure CLI and SWA CLI)

### Running Locally2. Builds the application with `npm run build`

3. Ensures configuration files are in place

1. **Start the Backend**:4. Gets the deployment token from Azure

   ```bash5. Deploys to Azure Static Web App

   cd backend6. Displays the URL where your app is available

   ./mvnw spring-boot:run

   ```After successful deployment, your Mastermind game will be available at:

   Backend runs on http://localhost:8080https://nice-sand-04c84f41e.1.azurestaticapps.net (or your custom domain if configured)


2. **Start the Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Frontend runs on http://localhost:3000

The frontend is configured with a proxy to forward `/api` requests to the backend.

### Building for Production

**Backend**:
```bash
cd backend
./mvnw clean package
```

**Frontend**:
```bash
cd frontend
npm run build
```

## Azure Deployment

This project is configured for deployment to **Azure Web App** with Java 21 runtime using Azure Developer CLI.

### Deploy to Azure

1. **Login to Azure**:
   ```bash
   azd auth login
   ```

2. **Initialize and Deploy**:
   ```bash
   azd up
   ```

This will:
- Provision Azure resources (App Service, Application Insights, Log Analytics)
- Build the Java application
- Deploy to Azure Web App
- Configure environment variables and CORS

### Infrastructure

The deployment creates:
- **Azure App Service** (Linux, Java 21) for the backend
- **Application Insights** for monitoring
- **Log Analytics Workspace** for logging

The frontend can be served statically or deployed separately to Azure Static Web Apps.

## Key Features

- **Drag & Drop Interface**: Drag colors from palette to slots or between slots
- **Real-time Feedback**: Immediate visual feedback for exact/partial matches
- **Game History**: View all previous attempts and their feedback
- **Solution Reveal**: Optional spoiler feature to see the secret code
- **Responsive Design**: Works on desktop and mobile devices
- **API-Driven**: Clean separation between frontend and backend
- **Azure-Ready**: Configured for cloud deployment with monitoring

## Technology Stack

**Backend**:
- Java 21
- Spring Boot 3.2
- Maven 3.9
- Jackson (JSON processing)
- SLF4J + Logback (logging)

**Frontend**:
- React 18
- TypeScript
- Vite
- CSS3 (custom styling)

**Infrastructure**:
- Azure App Service
- Azure Bicep
- Azure Developer CLI
- Application Insights

## Security Considerations

- CORS properly configured for frontend domain
- No hardcoded credentials
- Environment-based configuration
- HTTPS enforced in production
- Input validation on all API endpoints

## Monitoring

- Application Insights integration
- Structured logging
- Health check endpoints
- Performance metrics collection