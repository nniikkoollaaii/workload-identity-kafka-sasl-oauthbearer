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

/**
 * ...
 * 
 * <pre>   
 * // Use DefaultAzureCredential to support local environment workflows 
 * props.put("bearer.auth.credentials.source", "CUSTOM");
 * props.put("bearer.auth.custom.provider.class", "io.github.nniikkoollaaii.kafka.bearerauth.workload_identity.DefaultAzureCredentialBearerAuthCredentialProvider");
 * 
 * </pre>
 */
public class DefaultAzureCredentialBearerAuthCredentialProvider extends AbstractTokenCredentialBearerAuthCredentialProvider {
    /**
     * override this method in a subclass if you want to provide a custom 
     */
    @Override
    protected TokenCredential constructTokenCredential() {
        return WorkloadIdentityUtils.createDefaultAzureCredential(null);
    }
}