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

import java.net.URL;
import java.util.Map;
abstract class AbstractTokenCredentialBearerAuthCredentialProvider implements BearerAuthCredentialProvider {

    private static final Logger log = LoggerFactory.getLogger(WorkloadIdentityBearerAuthCredentialProvider.class);

    
    private TokenCredential tokencredential;
    private TokenRequestContext tokenRequestContext;
    private boolean isInitialized = false;

    @Override
    public String alias(){
        return "WORKLOAD_IDENTITY_OAUTHBEARER";
    }

    @Override
    public String getBearerToken(URL url) {
        log.debug("Get Token from AzureAD");
        checkInitialized();

        // AccessToken https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/AccessToken.java
        AccessToken azureIdentityAccessToken = tokencredential.getTokenSync(tokenRequestContext);
        String token = azureIdentityAccessToken.getToken();
        log.trace("Got token from AzureAD: '" + token + "'");
        return token;
    }
  
    @Override
    public void configure(Map<String, ?> map) {

        log.trace("Starting configuration process for WorkloadIdentityBearerAuthCredentialProvider");

        // Construct a WorkloadIdentityCredential object from Azure Identity SDK
        this.tokencredential = this.constructTokenCredential();

        // and construct a TokenRequestContext object from Azure Identity SDK
        this.tokenRequestContext = this.constructTokenRequestContext();

        isInitialized = true;
    }

    
    protected TokenCredential constructTokenCredential() {
        throw new UnsupportedOperationException("Not implemented in abstract base class");
    }

    protected TokenRequestContext constructTokenRequestContext() {
        return WorkloadIdentityUtils.createTokenRequestContextFromEnvironment();
    }


    private void checkInitialized() {
        if (!isInitialized)
            throw new IllegalStateException(String.format("To use %s, first call the configure or init method", getClass().getSimpleName()));
    }
}