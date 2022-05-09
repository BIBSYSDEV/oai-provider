# oai-provider

## Deployment

In AWS Cloudformation create stack using pipeline.yml

Set parameter 'OaiClientName' to 'DLR' or 'NVA'

Prerequisites in Secrets Manager:
* Secret named 'BackendCognitoClientCredentials' with key/value secrets 'backendClientId' and 'backendClientSecret' (Already present if deploying to NVA account, create secret with bogus values if DLR account)

Prerequisites in Systems Manager - Parameter Store:
* '/NVA/CognitoUri' (Already present if deploying to NVA account, create parameter with bogus value if DLR account)