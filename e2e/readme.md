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
    export SERVICE_ACCOUNT_ISSUER="http://nniikkoollaaii.github.io/workload-identity-kafka-sasl-oauthbearer"
    export SERVICE_ACCOUNT_KEY_FILE="$(pwd)/sa.pub"
    export SERVICE_ACCOUNT_SIGNING_KEY_FILE="$(pwd)/sa.key"
```


```
cat <<EOF | kind create cluster --name azure-workload-identity --image kindest/node:v1.22.4 --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  extraMounts:
    - hostPath: ${SERVICE_ACCOUNT_KEY_FILE}
      containerPath: /etc/kubernetes/pki/sa.pub
    - hostPath: ${SERVICE_ACCOUNT_SIGNING_KEY_FILE}
      containerPath: /etc/kubernetes/pki/sa.key
  kubeadmConfigPatches:
  - |
    kind: ClusterConfiguration
    apiServer:
      extraArgs:
        service-account-issuer: ${SERVICE_ACCOUNT_ISSUER}
        service-account-key-file: /etc/kubernetes/pki/sa.pub
        service-account-signing-key-file: /etc/kubernetes/pki/sa.key
    controllerManager:
      extraArgs:
        service-account-private-key-file: /etc/kubernetes/pki/sa.key
EOF
```