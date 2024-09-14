package io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity;


import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.TokenCredential;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.auth.SaslExtensions;
import org.apache.kafka.common.security.auth.SaslExtensionsCallback;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.apache.kafka.common.security.oauthbearer.internals.OAuthBearerClientInitialResponse;
import org.apache.kafka.common.security.oauthbearer.internals.secured.JaasOptionsUtils;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.sasl.SaslException;
import org.apache.kafka.common.config.ConfigException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.utils.AzureIdentityAccessTokenToKafkaClientOAuthBearerTokenMapper;
import io.github.nniikkoollaaii.kafka.workload_identity.*;


/**
 * Internal base class for this library.
 */
public abstract class AbstractTokenCredentialLoginCallbackHandler implements AuthenticateCallbackHandler {


    private static final Logger log = LoggerFactory.getLogger(AbstractTokenCredentialLoginCallbackHandler.class);



    private static final String EXTENSION_PREFIX = "extension_";
    private Map<String, Object> moduleOptions;
    private boolean isInitialized = false;

    private TokenCredential tokenCredential;
    private TokenRequestContext tokenRequestContext;


    /**
     * Configure this LoginCallbackHandler
     * @param configs
     * @param saslMechanism
     * @param jaasConfigEntries
     */
    @Override
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        log.trace("Starting configuration process for WorkloadIdentityLoginCallbackHandler");
        
        moduleOptions = JaasOptionsUtils.getOptions(saslMechanism, jaasConfigEntries);

        // Construct a TokenCredential object from Azure Identity SDK
        this.tokenCredential = this.constructTokenCredential();

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
            } else if (callback instanceof SaslExtensionsCallback) {
                handleExtensionsCallback((SaslExtensionsCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    protected void handleTokenCallback(OAuthBearerTokenCallback callback) {
        checkInitialized();

        log.debug("Get Token from AzureIdentity");
        // AccessToken https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/core/azure-core/src/main/java/com/azure/core/credential/AccessToken.java
        AccessToken azureIdentityAccessToken = tokenCredential.getTokenSync(tokenRequestContext);

        OAuthBearerToken token = AzureIdentityAccessTokenToKafkaClientOAuthBearerTokenMapper.map(azureIdentityAccessToken);
        
        log.trace("Got token from EntraId: '" + azureIdentityAccessToken.getToken() + "'");

        try {
            callback.token(token);
        } catch (ValidateException e) {
            log.warn(e.getMessage(), e);
            callback.error("invalid_token", e.getMessage(), null);
        }
    }

    /**
     * Code copied and adjusted from https://github.com/apache/kafka/blob/trunk/clients/src/main/java/org/apache/kafka/common/security/oauthbearer/OAuthBearerLoginCallbackHandler.java
     * @param callback
     */
    protected void handleExtensionsCallback(SaslExtensionsCallback callback) {

        Map<String, String> extensions = new HashMap<>();

        for (Map.Entry<String, Object> configEntry : this.moduleOptions.entrySet()) {
            String key = configEntry.getKey();

            if (!key.startsWith(EXTENSION_PREFIX))
                continue;

            Object valueRaw = configEntry.getValue();
            String value;

            if (valueRaw instanceof String)
                value = (String) valueRaw;
            else
                value = String.valueOf(valueRaw);

            extensions.put(key.substring(EXTENSION_PREFIX.length()), value);
        }

        SaslExtensions saslExtensions = new SaslExtensions(extensions);

        try {
            OAuthBearerClientInitialResponse.validateExtensions(saslExtensions);
        } catch (SaslException e) {
            throw new ConfigException(e.getMessage());
        }

        callback.extensions(saslExtensions);
    }

    private void checkInitialized() {
        if (!isInitialized)
            throw new IllegalStateException(String.format("To use %s, first call the configure or init method", getClass().getSimpleName()));
    }
}
