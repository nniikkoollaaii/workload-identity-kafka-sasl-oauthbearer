package io.github.nniikkoollaaii.kafka.bearerauth.workload_identity;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.WorkloadIdentityCredential;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.security.bearerauth.BearerAuthCredentialProvider;
import io.github.nniikkoollaaii.kafka.workload_identity.WorkloadIdentityUtils;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

/**
 * ...
 * 
 * <pre>   
 * // Use Workload Identity 
 * props.put("bearer.auth.credentials.source", "WORKLOAD_IDENTITY_OAUTHBEARER");
 * 
 * </pre>
 */
public class WorkloadIdentityBearerAuthCredentialProvider implements BearerAuthCredentialProvider {
    
    public static final String SASL_IDENTITY_POOL_CONFIG = "extension_identityPoolId";
    private static final Logger log = LoggerFactory.getLogger(WorkloadIdentityBearerAuthCredentialProvider.class);
    private WorkloadIdentityCredential workloadIdentityCredential;
    private TokenRequestContext tokenRequestContext;
    private boolean isInitialized = false;
    private String targetSchemaRegistry;
    private String targetIdentityPoolId;

    @Override
    public String alias() {
        return "WORKLOAD_IDENTITY_OAUTHBEARER";
    }

    @Override
    public String getBearerToken(URL url) {
        log.debug("Get Token from AzureAD");
        checkInitialized();

        // AccessToken https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/AccessToken.java
        AccessToken azureIdentityAccessToken = workloadIdentityCredential.getTokenSync(tokenRequestContext);
        String token = azureIdentityAccessToken.getToken();
        log.trace("Got token from AzureAD: '" + token + "'");
        return token;
    }

    @Override
    public String getTargetSchemaRegistry() {
        return this.targetSchemaRegistry;
    }

    @Override
    public String getTargetIdentityPoolId() {
        return this.targetIdentityPoolId;
    }

    @Override
    public void configure(Map<String, ?> map) {

        log.trace("Starting configuration process for WorkloadIdentityBearerAuthCredentialProvider");

        ConfigurationUtils cu = new ConfigurationUtils(map);
        targetSchemaRegistry = cu.validateString(
                SchemaRegistryClientConfig.BEARER_AUTH_LOGICAL_CLUSTER, false);
        targetIdentityPoolId = cu.validateString(
                SchemaRegistryClientConfig.BEARER_AUTH_IDENTITY_POOL_ID, false);

        // Construct a WorkloadIdentityCredential object from Azure Identity SDK
        this.workloadIdentityCredential = WorkloadIdentityUtils.createWorkloadIdentityCredentialFromEnvironment();

        // and construct a TokenRequestContext object from Azure Identity SDK
        this.tokenRequestContext = WorkloadIdentityUtils.createTokenRequestContextFromEnvironment();

        isInitialized = true;

    }


    private void checkInitialized() {
        if (!isInitialized)
            throw new IllegalStateException(String.format("To use %s, first call the configure or init method", getClass().getSimpleName()));
    }
}