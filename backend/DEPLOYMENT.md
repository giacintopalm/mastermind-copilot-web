# Backend CI/CD Setup

This repository includes a GitHub Actions workflow that automatically deploys the backend to Azure App Service when changes are pushed to the `main` branch.

## Required GitHub Secrets

To enable the CI/CD pipeline, you need to configure the following secrets in your GitHub repository:

### 1. AZURE_WEBAPP_NAME

The name of your Azure App Service.

**Value:** `app-backend-y7teeb42qtz4k`

### 2. AZURE_WEBAPP_PUBLISH_PROFILE

The publish profile for your Azure App Service.

**How to get it:**

Run the following command in your terminal:

```bash
az webapp deployment list-publishing-profiles --name app-backend-y7teeb42qtz4k --resource-group rg-giac --xml
```

Copy the entire XML output and paste it as the secret value.

## Setting Up GitHub Secrets

1. Go to your GitHub repository
2. Click on **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret:
   - Name: `AZURE_WEBAPP_NAME`
   - Value: `app-backend-y7teeb42qtz4k`
   - Click **Add secret**
5. Add the second secret:
   - Name: `AZURE_WEBAPP_PUBLISH_PROFILE`
   - Value: [paste the XML output from the command above]
   - Click **Add secret**

## Workflow Trigger

The workflow is triggered:
- Automatically on push to `main` branch when files in the `backend/` directory change
- Manually via GitHub Actions UI (workflow_dispatch)

## What the Workflow Does

1. Checks out the code
2. Sets up Java 21 (Temurin distribution)
3. Builds the backend with Maven (`mvnw clean package -DskipTests`)
4. Renames the JAR to `app.jar`
5. Deploys to Azure App Service using the publish profile

## Local Testing

Before pushing, test your backend locally:

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/mastermind-backend-1.0.0.jar
```

The application should start on `http://localhost:8080/api`
