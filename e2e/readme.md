# E2E Test

## Docs

https://azure.github.io/azure-workload-identity/docs/topics/self-managed-clusters/examples/kind.html


## Test environment versions

CI uses KinD 0.33.0 with Kubernetes 1.37.0 (node image pinned by digest),
Helm 4.3.1 and the Workload Identity Helm chart 1.6.1. kubectl matches the
Kubernetes version. The KinD configuration targets kubeadm v1beta4.
Producer and consumer containers use Eclipse Temurin 25; the library build
continues to use JDK 11 to verify its Java compatibility.

## Broker token validation

Kafka 4.4 requires both an expected audience and an expected issuer for OAuth
JWT validation. The Compose configuration uses the Entra v2 issuer, matching
the existing Schema Registry gateway configuration. This is the issuer of the
Entra access token, not the Kubernetes issuer used for workload federation.

The issuer must exactly match the access token's `iss` claim. The resource app's
`requestedAccessTokenVersion` controls the token format, independently of the
token endpoint version. For Entra v1 access tokens, override it before running
the test:

```sh
export AZURE_TOKEN_ISSUER="https://sts.windows.net/f3292839-9228-4d56-a08c-6023c5d71e65/"
```

## Setup

### ServiceAccount Key Generation

https://azure.github.io/azure-workload-identity/docs/installation/self-managed-clusters/service-account-key-generation.html

    openssl genrsa -out sa.key 2048

    openssl rsa -in sa.key -pubout -out sa.pub

### OpenID Connect Issuer

https://azure.github.io/azure-workload-identity/docs/installation/self-managed-clusters/oidc-issuer.html

Create files as linked on the site above.

  ./azwi jwks --public-keys sa.pub --output-file jwks.json

Serve files via Github Pages -> copy files to your special <username> repo -> Result:

https://nniikkoollaaii.github.io/kafka-sasl-oauthbearer-workload-identity/.well-kown/openid-configuration

https://nniikkoollaaii.github.io/kafka-sasl-oauthbearer-workload-identity/openid/v1/jwks



### IdP configuration - here AzureAD

  export APPLICATION_NAME="kafka-producer"
  az ad sp create-for-rbac --name "${APPLICATION_NAME}"

  # Get the object ID of the AAD application
  export APPLICATION_OBJECT_ID="$(az ad app show --id ${APPLICATION_CLIENT_ID} --query id -otsv)"

  az ad app federated-credential create --id ${APPLICATION_OBJECT_ID} --parameters @params.json

### Install KinD and kubectl binaries

curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.33.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind


curl -LO "https://dl.k8s.io/release/v1.37.0/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-4
chmod 700 get_helm.sh
DESIRED_VERSION=v4.3.1 ./get_helm.sh

### Create KIND cluster

```
kind create cluster --image kindest/node:v1.24.0 --config kind.local.config
```


### Create Kafka cluster


#### Create custom CA for local development

(Without always getting SSLAuthenticationException (only with the consumer not with the producer) when connecting to the broker.)

see [ssl/readme.md](./ssl/readme.md)

#### Setup Cluster

Docs:

https://cwiki.apache.org/confluence/plugins/servlet/mobile?contentId=186877575#KIP768:ExtendSASL/OAUTHBEARERwithSupportforOIDC-BrokerConfiguration
https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html#sasl-oauthbearer-jwks-endpoint-url
https://docs.confluent.io/platform/current/installation/docker/config-reference.html#confluent-enterprise-ak-configuration
https://docs.confluent.io/platform/current/installation/docker/image-reference.html


  docker-compose -f docker-compose.yaml -f docker-compose.local.yaml up -d


## Mutating Admission Webhook

https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html#mutating-admission-webhook


  docker pull mcr.microsoft.com/oss/azure/workload-identity/webhook:v1.1.2
  kind load docker-image mcr.microsoft.com/oss/azure/workload-identity/webhook:v1.1.2

  helm repo add azure-workload-identity https://azure.github.io/azure-workload-identity/charts
  helm repo update
  helm install workload-identity-webhook azure-workload-identity/workload-identity-webhook \
    --namespace azure-workload-identity-system \
    --create-namespace \
    --set azureTenantID="f3292839-9228-4d56-a08c-6023c5d71e65" \
    --wait \
    --debug \
    -v=5 \
    --devel



  kubectl logs azure-wi-webhook-controller-manager-7664467bfc-5m2bm -n azure-workload-identity-system

### Schema Registry

  curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{"schema": "{\"type\": \"record\", \"name\": \"ExampleRecord\", \"fields\": [{\"name\": \"content\", \"type\": \"string\"}]}"}' \
    http://localhost:8081/subjects/nniikkoollaaii.topic-value/versions

    Test:
    curl http://localhost:8081/subjects

## Test producer

  cd test-producer

  mvn package

  docker build -t io.github.nniikkoollaaii.kafka-producer-app:1.0.0 .

  kind load docker-image io.github.nniikkoollaaii.kafka-producer-app:1.0.0


## Test consumer

  cd test-consumer

  mvn package

  docker build -t io.github.nniikkoollaaii.kafka-consumer-app:1.0.0 .

  kind load docker-image io.github.nniikkoollaaii.kafka-consumer-app:1.0.0

## Test Schema Registry AuthN

  cd schema-registry-authn

  docker build -t io.github.nniikkoollaaii.schema-registry-authn:1.0.0 .

## Run Consumer and producer

  kubectl apply -f manifests/

  kubectl logs -f test-producer -n test
  kubectl logs -f test-consumer -n test

## Run Consumer and producer locally

docker-compose -f docker-compose.yaml -f docker-compose.local.yaml -f docker-compose.local.apps.yaml -f docker-compose.local.apps.secret.yaml  up -d