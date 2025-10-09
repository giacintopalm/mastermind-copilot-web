targetScope = 'subscription'

@minLength(1)
@maxLength(64)
@description('Name of the environment that can be used as part of naming resource convention')
param environmentName string

@minLength(1)
@description('Primary location for all resources')
param location string

// Generate a unique token to be used in naming resources
var resourceToken = toLower(uniqueString(subscription().id, environmentName, location))

// Organize resources in a resource group
resource resourceGroup 'Microsoft.Resources/resourceGroups@2021-04-01' = {
  name: 'rg-${environmentName}'
  location: location
  tags: {
    'azd-env-name': environmentName
  }
}

// Create User-Assigned Managed Identity (required by AZD rules)
module managedIdentity './core/security/managed-identity.bicep' = {
  name: 'managed-identity'
  scope: resourceGroup
  params: {
    name: 'id-${environmentName}-${resourceToken}'
    location: location
  }
}

// The application backend
module backend './core/host/appservice.bicep' = {
  name: 'backend'
  scope: resourceGroup
  params: {
    name: 'app-backend-${resourceToken}'
    location: location
    tags: {
      'azd-service-name': 'backend'
    }
    runtimeName: 'java'
    runtimeVersion: '21'
    appCommandLine: 'java -jar /home/site/wwwroot/app.jar'
    scmDoBuildDuringDeployment: false
  }
}

// App outputs
output APPLICATIONINSIGHTS_CONNECTION_STRING string = backend.outputs.applicationInsightsConnectionString
output AZURE_LOCATION string = location
output AZURE_TENANT_ID string = tenant().tenantId
output BACKEND_URI string = backend.outputs.uri
output RESOURCE_GROUP_ID string = resourceGroup.id
