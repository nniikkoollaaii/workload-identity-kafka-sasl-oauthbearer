package io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.utils;

/**
 * Custom runtime exception to signal errors when using Workload Identity to fetch a token from AzureAD.
 */
public class WorkloadIdentityKafkaClientOAuthBearerAuthenticationException extends RuntimeException {

    public WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String message) {
        super(message);
    }
}