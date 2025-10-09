@description('The name of the app service')
param name string

@description('The location into which the resources should be deployed')
param location string = resourceGroup().location

@description('The tags to apply to the resources')
param tags object = {}

@description('The name of the app service plan')
param appServicePlanName string = ''

@description('The name of the app service plan SKU')
param sku object = {
  name: 'B1'
  capacity: 1
}

@description('The runtime name')
param runtimeName string

@description('The runtime version')
param runtimeVersion string

@description('The app command line')
param appCommandLine string = ''

@description('Whether to enable SCM build during deployment')
param scmDoBuildDuringDeployment bool = false

@description('Application Insights name')
param applicationInsightsName string = ''

@description('Log Analytics workspace name')
param logAnalyticsName string = ''

// Generate unique names for supporting resources
var abbrs = loadJsonContent('../abbreviations.json')

var actualAppServicePlanName = !empty(appServicePlanName) ? appServicePlanName : '${abbrs.webServerFarms}${name}'
var actualApplicationInsightsName = !empty(applicationInsightsName) ? applicationInsightsName : '${abbrs.insightsComponents}${name}'
var actualLogAnalyticsName = !empty(logAnalyticsName) ? logAnalyticsName : '${abbrs.operationalInsightsWorkspaces}${name}'

// Log Analytics workspace
resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: actualLogAnalyticsName
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    features: {
      searchVersion: 1
      legacy: 0
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

// Application Insights
resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: actualApplicationInsightsName
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalytics.id
  }
}

// App Service Plan
resource appServicePlan 'Microsoft.Web/serverfarms@2022-03-01' = {
  name: actualAppServicePlanName
  location: location
  tags: tags
  sku: sku
  kind: 'linux'
  properties: {
    reserved: true
  }
}

// App Service
resource appService 'Microsoft.Web/sites@2022-03-01' = {
  name: name
  location: location
  tags: tags
  kind: 'app,linux'
  properties: {
    serverFarmId: appServicePlan.id
    siteConfig: {
      linuxFxVersion: '${toUpper(runtimeName)}|${runtimeVersion}'
      appCommandLine: appCommandLine
      scmType: 'None'
      ftpsState: 'FtpsOnly'
      minTlsVersion: '1.2'
      http20Enabled: true
      alwaysOn: true
      appSettings: [
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: applicationInsights.properties.ConnectionString
        }
        {
          name: 'SCM_DO_BUILD_DURING_DEPLOYMENT'
          value: string(scmDoBuildDuringDeployment)
        }
        {
          name: 'WEBSITES_ENABLE_APP_SERVICE_STORAGE'
          value: 'false'
        }
        // CORS settings for the React frontend
        {
          name: 'CORS_ALLOWED_ORIGINS'
          value: 'https://${name}.azurewebsites.net'
        }
      ]
    }
    httpsOnly: true
    clientAffinityEnabled: false
  }
}

// Configure logging
resource appServiceLogs 'Microsoft.Web/sites/config@2022-03-01' = {
  name: 'logs'
  parent: appService
  properties: {
    applicationLogs: {
      fileSystem: {
        level: 'Warning'
      }
    }
    httpLogs: {
      fileSystem: {
        retentionInMb: 40
        enabled: true
      }
    }
    failedRequestsTracing: {
      enabled: true
    }
    detailedErrorMessages: {
      enabled: true
    }
  }
}

// Output
output defaultHostName string = appService.properties.defaultHostName
output name string = appService.name
output uri string = 'https://${appService.properties.defaultHostName}'
output applicationInsightsConnectionString string = applicationInsights.properties.ConnectionString
output applicationInsightsInstrumentationKey string = applicationInsights.properties.InstrumentationKey
