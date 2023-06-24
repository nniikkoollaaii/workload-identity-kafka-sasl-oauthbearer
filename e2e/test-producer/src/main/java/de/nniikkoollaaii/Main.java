package io.github.nniikkoollaaii;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static final String TOPIC_NAME = "nniikkoollaaii.topic";

    public static void main(String[] args) {

        // Kafka producer configuration
        Properties props = new Properties();
        props.put("bootstrap.servers", System.getenv("BOOTSTRAP_SERVERS"));
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        //Use custom truststore in local setup
        props.put("ssl.truststore.location", System.getenv("KAFKA_SSL_TRUSTSTORE_LOCATION"));
        props.put("ssl.truststore.password", System.getenv("KAFKA_SSL_TRUSTSTORE_PASSWORD"));
        props.put("ssl.endpoint.identification.algorithm", "");
        
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "OAUTHBEARER");
        

        //Normal OAuth2
        //props.put("sasl.oauthbearer.token.endpoint.url", "https://login.microsoftonline.com/f3292839-9228-4d56-a08c-6023c5d71e65/oauth2/v2.0/token");
        //props.put("sasl.login.callback.handler.class", "org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler");
        //props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId='"+ System.getenv("CLIENT_ID") + "' scope='" + System.getenv("CLIENT_ID") + "/.default' clientSecret='" + System.getenv("CLIENT_SECRET") + "';");
        //props.put("sasl.oauthbearer.scope.claim.name", "scp");
        
        // Use Workload Identity 
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
        props.put("sasl.login.callback.handler.class", "io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.WorkloadIdentityLoginCallbackHandler");

        // Create the Kafka producer
        Producer<String, String> producer = new KafkaProducer<>(props);


        String event = System.getenv("MESSAGE_CONTENT");

        // Publish the event to the Kafka topic
        producer.send(new ProducerRecord<>(TOPIC_NAME, event));
        producer.flush();
        System.out.println("Published event: " + event);

        // Clean up and close the Kafka producer
        producer.close();
    }
}
