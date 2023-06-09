package io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity;


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

import io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.utils.AzureIdentityAccessTokenToKafkaClientOAuthBearerTokenMapper;
import io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.utils.WorkloadIdentityKafkaClientOAuthBearerAuthenticationException;

/**
 * This class implements the {@link AuthenticateCallbackHandler} of the Kafka client library. This interface is used in kafka clients as a callback handler to authenticate to a kafka broker.
 * This implementation is to be used in environments supporting <a href="https://azure.github.io/azure-workload-identity/docs/introduction.html">AzureAD Workload Identity</a>
 * 
 * It uses the ENV vars set by AzureAD Workload Identity Mutating Admission Webhook (AZURE_FEDERATED_TOKEN_FILE, AZURE_AUTHORITY_HOST), @see <a href="https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html">docs</a>, and the <a href="https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity">Azure Identity SDK for Java</a> {@link com.azure.identity.WorkloadIdentityCredential} to get an JWT token from AzureAD 
 * to be used in the kafka SASL OAUTHBEARER mechanism to authenticate to the kafka broker.
 * 
 * Use this implementation like so
 * 
 * <pre>
 * props.put("security.protocol", "SASL_SSL");
 * props.put("sasl.mechanism", "OAUTHBEARER");
 *   
 * // Use Workload Identity 
 * props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
 * props.put("sasl.login.callback.handler.class", "io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.WorkloadIdentityLoginCallbackHandler");
 * 
 * </pre>
 */
public class WorkloadIdentityLoginCallbackHandler implements AuthenticateCallbackHandler {


    private static final Logger log = LoggerFactory.getLogger(WorkloadIdentityLoginCallbackHandler.class);

    // ENV vars set by AzureAD Workload Identity Mutating Admission Webhook: https://azure.github.io/azure-workload-identity/docs/installation/mutating-admission-webhook.html
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE = "AZURE_FEDERATED_TOKEN_FILE";
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST = "AZURE_AUTHORITY_HOST";
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID = "AZURE_TENANT_ID";
    private static final String AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID = "AZURE_CLIENT_ID";


    private WorkloadIdentityCredential workloadIdentityCredential;
    private TokenRequestContext tokenRequestContext;


    /**
     * Constructor reading the required ENV vars to initialize required objects.
     */
    public WorkloadIdentityLoginCallbackHandler() {
        log.trace("Starting configuration process for WorkloadIdentityLoginCallbackHandler");

        // Construct a WorkloadIdentityCredential object from Azure Identity SDK
        String federatedTokeFilePath = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE);
        if (federatedTokeFilePath == null || federatedTokeFilePath.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_FEDERATED_TOKEN_FILE));
        log.info("Federated Token File at path " + federatedTokeFilePath);
        
        String authorityHost = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST);
        if (authorityHost == null || authorityHost.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_AUTHORITY_HOST));
        log.info("Authority host " + authorityHost);
        
        String tenantId = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID);
        if (tenantId == null || tenantId.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_TENANT_ID));
        log.info("Tenant Id " + tenantId);
        
        String clientId = System.getenv(AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID);
        if (clientId == null || clientId.equals(""))
            throw new WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String.format("Missing environment variable %s", AZURE_AD_WORKLOAD_IDENTITY_MUTATING_ADMISSION_WEBHOOK_ENV_CLIENT_ID));
        log.info("Client Id " + clientId);
        
        
        workloadIdentityCredential = new WorkloadIdentityCredentialBuilder()
                .tokenFilePath(federatedTokeFilePath)
                .authorityHost(authorityHost)
                .clientId(clientId)
                .tenantId(tenantId)
                .build();


        //Construct a TokenRequestContext to be used be requsting a token at runtime.
        //ToDo: make Scope configurable to get access token for e.g. App Registration Kafka Cluster
        String defaultScope =  clientId + "/.default";
        log.info("Scope " + defaultScope);
        tokenRequestContext = new TokenRequestContext() // TokenRequestContext: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/TokenRequestContext.java
                .addScopes(defaultScope)
                .setTenantId(tenantId);

    }

    // This method is not called in the e2e test ... ?! Why? -> setup in constructor
    /**
     * not used
     * @param map
     * @param s
     * @param list
     */
    @Override
    public void configure(Map<String, ?> map, String s, List<AppConfigurationEntry> list) {
        //nop required for WorkloadIdentityCredential
        log.trace("configure WorkloadIdentityLoginCallbackHandler");
    }

    /**
     * not used
     */
    @Override
    public void close() {
        //nop required for WorkloadIdentityCredential
        log.trace("close WorkloadIdentityLoginCallbackHandler");
    }


    /**
     * Called by the kafka client. The magic happens here ...
     * The token is fetched synchronizly from AzureAD and returned to the callback object
     * @param callbacks
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback) {
                handleTokenCallback((OAuthBearerTokenCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private void handleTokenCallback(OAuthBearerTokenCallback callback) throws IOException {
        log.trace("handleTokenCallback - get Token from AzureAD");

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
}
