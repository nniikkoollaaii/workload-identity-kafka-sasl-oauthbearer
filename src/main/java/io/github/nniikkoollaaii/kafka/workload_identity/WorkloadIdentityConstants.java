package io.github.nniikkoollaaii.kafka.workload_identity;

public class WorkloadIdentityConstants {
        // ENV vars set by AzureAD Workload Identity Mutating Admission Webhook: https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE = "AZURE_FEDERATED_TOKEN_FILE";
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST = "AZURE_AUTHORITY_HOST";
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID = "AZURE_TENANT_ID";
        public static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID = "AZURE_CLIENT_ID";
    
}