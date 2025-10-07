# AKS Workload Identity to Kafka SASL OAUTHBEARER Login Callback Handler

Kafka Client Login Callback Handler to be used in Kafka Clients authenticating to an OAuth2 enabled Kafka Broker AND running on an Azure Service with Workload Identity enabled.

This LoginCallbackHandler is a replacement for the default included [OAuthBearerLoginCallbackHandler](https://github.com/apache/kafka/blob/trunk/clients/src/main/java/org/apache/kafka/common/security/oauthbearer/OAuthBearerLoginCallbackHandler.java)

This LoginCallbackHandler uses the Environment variables [defined in the Mutating Admission Webhook Controller](https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html) for AKS Workload Identity to configure the Azure Identity [WorkloadIdentityCredential](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/identity/azure-identity/src/main/java/com/azure/identity/WorkloadIdentityCredential.java).


## Usage

Configure this Kafka Client Login Callback Handler to be used by setting

```
sasl.login.callback.handler.class=io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.WorkloadIdentityLoginCallbackHandler
```



## Testing 

see [here](./e2e/readme.md)

## ToDo

- Make scope configurable

- Make env names configurable

- Implement other configuration options than reading from env vars. Like extension attributes with value for e.g. token file path or client id. 

## Develop

    mvn install

### Publishing to Maven Central

See deploy.yaml Workflow

HowTo Deploy: https://central.sonatype.org/publish/publish-portal-maven/

HowTo GPG Signing: https://central.sonatype.org/publish/requirements/gpg/#generating-a-key-pair

Published to KeyServer:
- https://keys.openpgp.org/search?q=A94FE42A09D210C8038CDEDECA796C3D12730018
- https://keyserver.ubuntu.com/pks/lookup?search=A94FE42A09D210C8038CDEDECA796C3D12730018&fingerprint=on&op=index

Result:
- Maven Central Publish Status: https://central.sonatype.com/publishing/deployments
- Maven Central Index: https://repo1.maven.org/maven2/io/github/nniikkoollaaii/kafka-sasl-oauthbearer-workload-identity/






