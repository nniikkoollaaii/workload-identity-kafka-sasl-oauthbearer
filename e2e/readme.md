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

Serve files via Github Repo API:

http://nniikkoollaaii.github.io/workload-identity-kafka-sasl-oauthbearer/e2e/public/.well-kown/openid-configuration

http://nniikkoollaaii.github.io/workload-identity-kafka-sasl-oauthbearer/e2e/public/openid/v1/jwks

