4# AKS Workload Identity to Kafka SASL OAUTHBEARER Login Callback Handler

Kafka Client Login Callback Handler to be used in Kafka Clients authenticating to an OAuth2 enabled Kafka Broker AND
running on an Azure Service with Workload Identity enabled.

This LoginCallbackHandler is a replacement for the default
included [OAuthBearerLoginCallbackHandler](https://github.com/apache/kafka/blob/trunk/clients/src/main/java/org/apache/kafka/common/security/oauthbearer/OAuthBearerLoginCallbackHandler.java)

This LoginCallbackHandler uses the Environment
variables [defined in the Mutating Admission Webhook Controller](https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html)
for AKS Workload Identity to configure the Azure
Identity [WorkloadIdentityCredential](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/identity/azure-identity/src/main/java/com/azure/identity/WorkloadIdentityCredential.java).

## Usage

Configure this Kafka Client Login Callback Handler to be used by setting

```
sasl.login.callback.handler.class=io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.WorkloadIdentityLoginCallbackHandler
```

Per default in the TokenRequest to EntraId the scope "<clientId>/.default" is used. If you need to configure a custom
scope, then configure it's value via the ENV "AZURE_TOKEN_REQUEST_SCOPE".

## Testing

see [here](./e2e/readme.md)

## ToDo

- Make scope configurable

- Make env names configurable

- Implement other configuration options than reading from env vars. Like extension attributes with value for e.g. token
  file path or client id.

## Develop

    mvn install

### Branching and maintenance releases

Each Kafka minor version has its own maintenance branch:

| Branch | Purpose | Example release |
| --- | --- | --- |
| `main` | Development for the newest supported Kafka minor | `v4.4.0` |
| `release/4.4` | Patches for Kafka 4.4.x | `v4.4.1` |
| `release/4.3` | Patches for Kafka 4.3.x | `v4.3.2` |
| `release/4.2` | Patches for Kafka 4.2.x | `v4.2.2` |

A lower minor version can be published after a higher one. Maven versions are
independent; publishing 4.3.1 does not prevent publishing 4.2.2.
The library major/minor identifies the Kafka compatibility line. Its patch number
increases for every library release, including fixes without a Kafka upgrade;
it does not have to equal the Kafka dependency patch number.

Create a maintenance branch from the last release tag of that line, not from
`main` after it has moved to a newer Kafka minor:

```sh
git switch -c release/4.2 v4.2.1
```

For existing tags, also bring the updated `.github/workflows/ci.yaml` and
`.github/workflows/deploy.yaml` from this branching change onto the maintenance
branch before publishing. Old tags contain the old workflows. Commit these
workflow changes on the maintenance branch.

For each patch:

1. Create a fix branch from the appropriate `release/<major>.<minor>` branch.
2. Apply the fix (cherry-pick individual fix commits when backporting). Keep Kafka
   and Confluent dependencies compatible with that minor line; do not merge
   `main` wholesale into older maintenance branches.
3. Set the root POM library version to the next unused patch, e.g. `4.2.2`.
   Update dependencies as needed and update the library dependency in the E2E
   POMs to the same library version. Keep the E2E Kafka dependencies and broker
   images compatible with the maintenance line.
4. Open a PR targeting `release/4.2` and let CI pass.
5. Tag the merged commit as `v4.2.2` and publish a GitHub Release for that tag.
   For older lines, leave GitHub's “Set as the latest release” option disabled.

When `main` moves to another Kafka minor, first create the maintenance branch
for the previous minor from its latest release tag. Apply relevant fixes to all
supported lines via separate PRs.

### Publishing to Maven Central

The `deploy.yaml` workflow runs when a GitHub Release is published. It checks out
the release tag and verifies that `v<major>.<minor>.<patch>` matches the root POM
version without the `v` prefix. It publishes that POM version unchanged.

For a manual run, select the updated workflow on `main` and supply an existing
release tag in the required `tag` input. The build uses that tag's source, even
when the tag belongs to an older maintenance line. Published Maven Central
versions cannot be overwritten; use a new patch version for subsequent changes.

#### Additional Infos

HowTo Deploy: https://central.sonatype.org/publish/publish-portal-maven/

HowTo GPG Signing: https://central.sonatype.org/publish/requirements/gpg/#generating-a-key-pair

Published to KeyServer:

- https://keys.openpgp.org/search?q=A94FE42A09D210C8038CDEDECA796C3D12730018
- https://keyserver.ubuntu.com/pks/lookup?search=A94FE42A09D210C8038CDEDECA796C3D12730018&fingerprint=on&op=index

Result:

- Maven Central Publish Status: https://central.sonatype.com/publishing/deployments
- Maven Central Index: https://repo1.maven.org/maven2/io/github/nniikkoollaaii/kafka-sasl-oauthbearer-workload-identity/







