package io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity;


import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.WorkloadIdentityCredential;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
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
 * This class implements the {@link AuthenticateCallbackHandler} interface of the Kafka client library. This interface is used in kafka clients as a callback handler to authenticate to a kafka broker.
 * This implementation is to be used in environments supporting <a href="https://azure.github.io/azure-workload-identity/docs/introduction.html">AzureAD Workload Identity</a>
 * 
 * It uses the @{link com.azure.identity.DefaultAzureCredential}, see <a href="https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity">Azure Identity SDK for Java</a> to get an JWT token from EntraId using different TokenCredential implementations supporting other authentication mechanisms for e.g. local dev environment workflows via AzureCliCredential
 * This class is to be used in the kafka SASL OAUTHBEARER mechanism to authenticate to the kafka broker.
 * 
 * Use this implementation like so
 * 
 * <pre>
 * props.put("security.protocol", "SASL_SSL");
 * props.put("sasl.mechanism", "OAUTHBEARER");
 *   
 * // Use Workload Identity 
 * props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
 * props.put("sasl.login.callback.handler.class", "io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.DefaultAzureCredentialLoginCallbackHandler");
 * 
 * </pre>
 */
public class DefaultAzureCredentialLoginCallbackHandler extends AbstractTokenCredentialLoginCallbackHandler {

    /**
     * override this method in a subclass if you want to provide a custom 
     */
    @Override
    protected TokenCredential constructTokenCredential() {
        return WorkloadIdentityUtils.createDefaultAzureCredential(null);
    }
}
