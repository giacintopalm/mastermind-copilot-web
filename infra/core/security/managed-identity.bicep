@description('The name of the managed identity')
param name string

@description('The location into which the resources should be deployed')
param location string = resourceGroup().location

@description('The tags to apply to the resources')
param tags object = {}

// Create User-Assigned Managed Identity
resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: name
  location: location
  tags: tags
}

// Outputs
output name string = managedIdentity.name
output id string = managedIdentity.id
output clientId string = managedIdentity.properties.clientId
output principalId string = managedIdentity.properties.principalId
