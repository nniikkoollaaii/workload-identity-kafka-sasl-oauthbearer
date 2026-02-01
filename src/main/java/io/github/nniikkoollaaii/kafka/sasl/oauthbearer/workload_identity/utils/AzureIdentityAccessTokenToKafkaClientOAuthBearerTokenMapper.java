package io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.utils;

import com.azure.core.credential.AccessToken;
import org.apache.kafka.common.security.oauthbearer.JwtValidatorException;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.internals.secured.BasicOAuthBearerToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static org.apache.kafka.common.security.oauthbearer.ClientJwtValidator.EXPIRATION_CLAIM_NAME;
import static org.apache.kafka.common.security.oauthbearer.ClientJwtValidator.ISSUED_AT_CLAIM_NAME;

/**
 * Mapper class mapping an {@link com.azure.core.credential.AccessToken} returned by the Azure Identity SDK to an {@link org.apache.kafka.common.security.oauthbearer.internals.secured.BasicOAuthBearerToken} expected by the kafka client sasl {@link org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback}
 */
public class AzureIdentityAccessTokenToKafkaClientOAuthBearerTokenMapper {

    public static final String scopeClaimName = "scp";
    public static final String subClaimName = "sub";
    private static final Logger log = LoggerFactory.getLogger(AzureIdentityAccessTokenToKafkaClientOAuthBearerTokenMapper.class);

    public static OAuthBearerToken map(AccessToken azureIdentityAccessToken){
        // Parse the JWT claims to set required values on Kafka Client OAuthBearer Token Object
        log.trace("Map Azure Identity Access Token to Kafka Client OAuthBearerToken");

        // Construct an SOSE JWT Consumer to parse the JWT
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setSkipSignatureVerification() //already checked by Azure Identity SDK
                .setSkipAllValidators() //already checked by Azure Identity SDK
                .build();

        //Parse it
        JwtClaims claims =  null;
        try {
            claims = jwtConsumer.processToClaims(azureIdentityAccessToken.getToken());
        } catch (InvalidJwtException e) {
            throw new JwtValidatorException(String.format("Could not validate the access token: %s", e.getMessage()), e);
        }
        Map<String, Object> payload = claims.getClaimsMap();

        //following code is borrowed from org.apache.kafka.common.security.oauthbearer.internals.securedLoginAccessTokenValidator
        //ToDo: make scope claim name configurable
        Object scopeRaw = getClaim(payload, scopeClaimName);
        Collection<String> scopeRawCollection;

        if (scopeRaw instanceof String)
            scopeRawCollection = Collections.singletonList((String) scopeRaw);
        else if (scopeRaw instanceof Collection)
            scopeRawCollection = (Collection<String>) scopeRaw;
        else
            scopeRawCollection = Collections.emptySet();

        Number expirationTimeRaw = (Number) getClaim(payload, EXPIRATION_CLAIM_NAME);
        //ToDo: make subject claim name configurable
        String subRaw = (String) getClaim(payload, subClaimName);
        Number issuedAtRaw = (Number) getClaim(payload, ISSUED_AT_CLAIM_NAME);


        // The token's lifetime, expressed as the number of milliseconds since the poch. Must be non-negative
        // https://github.com/a0x8o/kafka/blob/master/clients/src/main/java/org/apache/kafka/common/security/oauthbearer/secured/BasicOAuthBearerToken.java#L55
        // AzureAD expiry claim: Timestamp in unix epoch Seconds where the token expires -> duration via extration with issuedAt
        //  https://github.com/uglide/azure-content/blob/master/articles/active-directory/active-directory-token-and-claims.md
        // and multiple with 1000 to convert to milliseconds
        Long lifetimeMs = expirationTimeRaw.longValue() * 1000;
        
        // When the credential became valid, in terms of the number of milliseconds since the epoch
        // https://github.com/a0x8o/kafka/blob/master/clients/src/main/java/org/apache/kafka/common/security/oauthbearer/secured/BasicOAuthBearerToken.java#L148
        Long startTimeMs = issuedAtRaw.longValue() * 1000; 


        // construct an Kafka Client OAuth Bearer Token and return it
        OAuthBearerToken token = new BasicOAuthBearerToken(
                azureIdentityAccessToken.getToken(),
                new HashSet<>(scopeRawCollection),
                lifetimeMs,
                subRaw,
                startTimeMs);
        return token;
    }

    private static Object getClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        log.debug("getClaim - {}: {}", claimName, value);
        return value;
    }
}
