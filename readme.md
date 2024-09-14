# AKS Workload Identity to Kafka SASL OAUTHBEARER Login Callback Handler

Kafka Client Login Callback Handler to be used in Kafka Clients authenticating to an OAuth2 enabled Kafka Broker AND running on an Azure Service with Workload Identity enabled.

This LoginCallbackHandler is a replacement for the default included [OAuthBearerLoginCallbackHandler](https://github.com/apache/kafka/blob/trunk/clients/src/main/java/org/apache/kafka/common/security/oauthbearer/OAuthBearerLoginCallbackHandler.java)

This LoginCallbackHandler uses the Environment variables [defined in the Mutating Admission Webhook Controller](https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html) for AKS Workload Identity to configure the Azure Identity [WorkloadIdentityCredential](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/identity/azure-identity/src/main/java/com/azure/identity/WorkloadIdentityCredential.java).


## Usage

Configure this Kafka Client Login Callback Handler to be used by setting

```
sasl.login.callback.handler.class=io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.WorkloadIdentityLoginCallbackHandler

bearer.auth.credentials.source=CUSTOM
bearer.auth.custom.provider.class=io.github.nniikkoollaaii.kafka.bearerauth.workload_identity.WorkloadIdentityBearerAuthCredentialProvider 
```

## v2 Migration Guide

v2 of this Lib is using the "CUSTOM" BearerAuthCredentialProvider implementation so change your config from

    bearer.auth.credentials.source=WORKLOAD_IDENTITY_OAUTHBEARER

to

    bearer.auth.credentials.source=CUSTOM
    bearer.auth.custom.provider.class=io.github.nniikkoollaaii.kafka.bearerauth.workload_identity.WorkloadIdentityBearerAuthCredentialProvider 


## Testing 

see [here](./e2e/readme.md)

## ToDo

- Make scope configurable

- Make env names configurable

- Implement other configuration options than reading from env vars. Like extension attributes with value for e.g. token file path or client id. 

## Develop

    mvn install
