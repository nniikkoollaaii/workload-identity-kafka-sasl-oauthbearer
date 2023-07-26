package io.github.nniikkoollaaii.kafka.bearerauth.workload_identity;

import io.confluent.kafka.schemaregistry.client.security.bearerauth.BearerAuthCredentialProvider;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.nniikkoollaaii.kafka.workload_identity.WorkloadIdentityKafkaClientOAuthBearerAuthenticationException;
import io.github.nniikkoollaaii.kafka.workload_identity.*;

/**
 * ...
 * 
 * <pre>   
 * // Use Workload Identity 
 * props.put("bearer.auth.credentials.source", "CUSTOM");
 * props.put("bearer.auth.custom.provider.class", "io.github.nniikkoollaaii.kafka.bearerauth.workload_identity.WorkloadIdentityBearerAuthCredentialProvider");
 * 
 * </pre>
 */
public class WorkloadIdentityBearerAuthCredentialProvider implements BearerAuthCredentialProvider {
    
    private static final Logger log = LoggerFactory.getLogger(WorkloadIdentityBearerAuthCredentialProvider.class);

    
    private WorkloadIdentityCredential workloadIdentityCredential;
    private TokenRequestContext tokenRequestContext;

    @Override
    public String getBearerToken(URL url) {
        // AccessToken https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/AccessToken.java
        AccessToken azureIdentityAccessToken = workloadIdentityCredential.getTokenSync(tokenRequestContext);

        return azureIdentityAccessToken.getToken();
    }
  
    @Override
    public void configure(Map<String, ?> map) {

        log.trace("Starting configuration process for WorkloadIdentityBearerAuthCredentialProvider");
        
        //ConfigurationUtils cu = new ConfigurationUtils(map);

        // Construct a WorkloadIdentityCredential object from Azure Identity SDK
        String federatedTokeFilePath = System.getenv(WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE);
        if (federatedTokeFilePath == null || federatedTokeFilePath.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE));
        log.debug("Config: Federated Token File Path " + federatedTokeFilePath);
        
        String authorityHost = System.getenv(WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST);
        if (authorityHost == null || authorityHost.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST));
            log.debug("Config: Authority host " + authorityHost);
        
        String tenantId = System.getenv(WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID);
        if (tenantId == null || tenantId.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID));
            log.debug("Config: Tenant Id " + tenantId);
        
        String clientId = System.getenv(WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID);
        if (clientId == null || clientId.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", WorkloadIdentityConstants.AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID));
            log.debug("Config: Client Id " + clientId);
        
        
        workloadIdentityCredential = new WorkloadIdentityCredentialBuilder()
                .tokenFilePath(federatedTokeFilePath)
                .authorityHost(authorityHost)
                .clientId(clientId)
                .tenantId(tenantId)
                .build();


        //Construct a TokenRequestContext to be used be requsting a token at runtime.
        //ToDo: make Scope configurable to get access token for e.g. App Registration Kafka Cluster
        String defaultScope =  clientId + "/.default";
        log.debug("Config: Scope " + defaultScope);
        tokenRequestContext = new TokenRequestContext() // TokenRequestContext: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/TokenRequestContext.java
                .addScopes(defaultScope)
                .setTenantId(tenantId);

    }
}