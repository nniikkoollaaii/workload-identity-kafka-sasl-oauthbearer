package de.nniikkoollaaii;


import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
public class WorkloadIdentityLoginCallbackHandler implements AuthenticateCallbackHandler {


    private static final Logger log = LoggerFactory.getLogger(WorkloadIdentityLoginCallbackHandler.class);

    // ENV vars set by AzureAD Workload Identity Mutating Admission Webhook: https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE = "AZURE_FEDERATED_TOKEN_FILE";
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST = "AZURE_AUTHORITY_HOST";
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID = "AZURE_TENANT_ID";
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID = "AZURE_CLIENT_ID";


    private WorkloadIdentityCredential workloadIdentityCredential;
    private TokenRequestContext tokenRequestContext;
    private boolean isInitialized = false;


    @Override
    public void configure(Map<String, ?> map, String s, List<AppConfigurationEntry> list) {
        log.trace("Starting configuration process for WorkloadIdentityLoginCallbackHandler");

        // Construct a WorkloadIdentityCredential from Azure Identity SDK
        String federatedTokeFilePath = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE);
        log.debug("Federated Token File at path " + federatedTokeFilePath);
        workloadIdentityCredential = new WorkloadIdentityCredentialBuilder()
                .tokenFilePath(federatedTokeFilePath)
                .build();

        //Construct a TokenRequestContext to be used be requsting a token at runtime.
        //ToDo: make Scope configurable to get access token for e.g. App Registration Kafka Cluster
        String defaultScope = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID) + "/.default";
        log.debug("Scope " + defaultScope);
        String tenantId = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID);
        log.debug("Tenant Id " + tenantId);
        tokenRequestContext = new TokenRequestContext() // TokenRequestContext: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/TokenRequestContext.java
                .addScopes(defaultScope)
                .setTenantId(tenantId);


        isInitialized = true;
        log.debug("Configured WorkloadIdentityLoginCallbackHandler successfully!");
    }

    @Override
    public void close() {
        //nop required for WorkloadIdentityCredential
        log.trace("close WorkloadIdentityLoginCallbackHandler");
    }


    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        checkInitialized();

        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback) {
                handleTokenCallback((OAuthBearerTokenCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private void handleTokenCallback(OAuthBearerTokenCallback callback) throws IOException {
        log.trace("WorkloadIdentityLoginCallbackHandler handleTokenCallback");
        checkInitialized();

        // AccessToken https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/AccessToken.java
        AccessToken azureIdentityAccessToken = workloadIdentityCredential.getTokenSync(tokenRequestContext);

        OAuthBearerToken token = AzureIdentityAccessTokenToKafkaClientOAuthBearerTokenMapper.map(azureIdentityAccessToken);

        try {
            callback.token(token);
        } catch (ValidateException e) {
            log.warn(e.getMessage(), e);
            callback.error("invalid_token", e.getMessage(), null);
        }
    }

    private void checkInitialized() {
        if (!isInitialized)
            log.trace("WorkloadIdentityLoginCallbackHandler handleTokenCallback called but not initialized!");
            throw new IllegalStateException(String.format("To use %s, first call the configure or init method", getClass().getSimpleName()));
    }
}
