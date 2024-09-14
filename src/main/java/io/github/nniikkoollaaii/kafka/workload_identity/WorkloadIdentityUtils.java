package io.github.nniikkoollaaii.kafka.workload_identity;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import com.azure.identity.TokenCredential;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkloadIdentityUtils {

        private static final Logger log = LoggerFactory.getLogger(WorkloadIdentityUtils.class);


        // ENV vars set by AzureAD Workload Identity Mutating Admission Webhook: https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE = "AZURE_FEDERATED_TOKEN_FILE";
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST = "AZURE_AUTHORITY_HOST";
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID = "AZURE_TENANT_ID";
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID = "AZURE_CLIENT_ID";


        public static String getTenantId() {
                String tenantId = System.getenv(WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID);
                if (tenantId == null || tenantId.equals(""))
                        throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID));
                log.debug("Config: Tenant Id " + tenantId);
                return tenantId;
        }

        public static String getClientId() {
                String clientId = System.getenv(WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID);
                if (clientId == null || clientId.equals(""))
                        throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID));
                log.debug("Config: Client Id " + clientId);
                return clientId;
        }

        public static WorkloadIdentityCredential createWorkloadIdentityCredentialFromEnvironment() {
                String federatedTokeFilePath = System.getenv(WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE);
                if (federatedTokeFilePath == null || federatedTokeFilePath.equals(""))
                        throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE));
                log.debug("Config: Federated Token File Path " + federatedTokeFilePath);

                String authorityHost = System.getenv(WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST);
                if (authorityHost == null || authorityHost.equals(""))
                        throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityUtils.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST));
                log.debug("Config: Authority host " + authorityHost);
                
                String tenantId = getTenantId(); 
                String clientId = getClientId();


                WorkloadIdentityCredential workloadIdentityCredential  = new WorkloadIdentityCredentialBuilder()
                        .tokenFilePath(federatedTokeFilePath)
                        .authorityHost(authorityHost)
                        .clientId(clientId)
                        .tenantId(tenantId)
                        .build();

                return workloadIdentityCredential;
        }

        public static DefaultAzureCredential createDefaultAzureCredential(DefaultAzureCredentialOptions defaultAzureCredentialOptions) {
                return new DefaultAzureCredential(defaultAzureCredentialOptions);
        }

        public static TokenRequestContext createTokenRequestContextFromEnvironment() {

                String tenantId = getTenantId(); 
                String clientId = getClientId();

                //Construct a TokenRequestContext to be used be requsting a token at runtime.
                //ToDo: make Scope configurable to get access token for e.g. App Registration Kafka Cluster
                String defaultScope =  clientId + "/.default";
                log.debug("Config: Scope " + defaultScope);
                TokenRequestContext tokenRequestContext = new TokenRequestContext() // TokenRequestContext: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/TokenRequestContext.java
                        .addScopes(defaultScope)
                        .setTenantId(tenantId);

                return tokenRequestContext;
        }
}