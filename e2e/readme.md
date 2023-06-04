# E2E Test

## Docs

https://azure.github.io/azure-workload-identity/docs/topics/self-managed-clusters/examples/kind.html

## Setup

### ServiceAccount Key Generation

https://azure.github.io/azure-workload-identity/docs/installation/self-managed-clusters/service-account-key-generation.html

    openssl genrsa -out sa.key 2048

    openssl rsa -in sa.key -pubout -out sa.pub

### OpenID Connect Issuer

https://azure.github.io/azure-workload-identity/docs/installation/self-managed-clusters/oidc-issuer.html

Create files as linked on the site above.

Serve files via Github Pages -> copy files to your special <username> repo -> Result:

https://nniikkoollaaii.github.io/workload-identity-kafka-sasl-oauthbearer/.well-kown/openid-configuration

https://nniikkoollaaii.github.io/workload-identity-kafka-sasl-oauthbearer/openid/v1/jwks



### Create KIND cluster

```
kind create cluster --name azure-workload-identity --image kindest/node:v1.22.4 --config-file kind.config
```


### Create Kafka cluster

Docs:

https://cwiki.apache.org/confluence/plugins/servlet/mobile?contentId=186877575#KIP768:ExtendSASL/OAUTHBEARERwithSupportforOIDC-BrokerConfiguration
https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html#sasl-oauthbearer-jwks-endpoint-url
https://docs.confluent.io/platform/current/installation/docker/config-reference.html#confluent-enterprise-ak-configuration
https://docs.confluent.io/platform/current/installation/docker/image-reference.html


  docker-compose up

### IdP configuration - here AzureAD

  export APPLICATION_NAME="kafka-producer"
  az ad sp create-for-rbac --name "${APPLICATION_NAME}"

  # Get the object ID of the AAD application
  export APPLICATION_OBJECT_ID="$(az ad app show --id ${APPLICATION_CLIENT_ID} --query id -otsv)"

  cat <<EOF > params.json
  {
    "name": "kubernetes-federated-credential",
    "issuer": "https://nniikkoollaaii.github.io/workload-identity-kafka-sasl-oauthbearer",
    "subject": "system:serviceaccount:test:sa-test",
    "description": "Kubernetes service account federated credential",
    "audiences": [
      "api://AzureADTokenExchange"
    ]
  }
  EOF

  az ad app federated-credential create --id ${APPLICATION_OBJECT_ID} --parameters @params.json


## Test producer

  cd test-producer

  mvn package

  docker build -t de.nniikkoollaaii.kafka-producer-app:1.0.0 .