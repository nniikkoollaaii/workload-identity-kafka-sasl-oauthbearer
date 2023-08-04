package io.github.nniikkoollaaii.kafka.workload_identity;

/**
 * Custom runtime exception to signal errors when using Workload Identity to fetch a token from AzureAD.
 */
public class WorkloadIdentityKafkaClientOAuthBearerAuthenticationException extends RuntimeException {

    public WorkloadIdentityKafkaClientOAuthBearerAuthenticationException(String message) {
        super(message);
    }
}