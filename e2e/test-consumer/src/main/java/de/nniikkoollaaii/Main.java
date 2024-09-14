package io.github.nniikkoollaaii;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Main {

    private static final String TOPIC_NAME = "nniikkoollaaii.topic";
    private static final String OUTPUT_FILE = "output.txt";

    public static void main(String[] args) {
        // Kafka consumer configuration
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("bootstrap.servers", System.getenv("BOOTSTRAP_SERVERS"));
        props.put("schema.registry.url", System.getenv("SCHEMA_REGISTRY_URL"));

        //Use custom truststore in local setup
        props.put("ssl.truststore.location", System.getenv("KAFKA_SSL_TRUSTSTORE_LOCATION"));
        props.put("ssl.truststore.password", System.getenv("KAFKA_SSL_TRUSTSTORE_PASSWORD"));
        props.put("ssl.endpoint.identification.algorithm", "");
        
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "OAUTHBEARER");
        
        //Normal OAuth2
        // props.put("sasl.oauthbearer.token.endpoint.url", "https://login.microsoftonline.com/f3292839-9228-4d56-a08c-6023c5d71e65/oauth2/v2.0/token");
        // props.put("sasl.login.callback.handler.class", "org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler");
        // props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId='"+ System.getenv("CLIENT_ID") + "' scope='" + System.getenv("CLIENT_ID") + "/.default' clientSecret='" + System.getenv("CLIENT_SECRET") + "';");
        // props.put("sasl.oauthbearer.scope.claim.name", "scp");
        
        // props.put("bearer.auth.logical.cluster", "123456");
        // props.put("bearer.auth.credentials.source", "SASL_OAUTHBEARER_INHERIT");
        
        // Use Workload Identity 
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");
        props.put("sasl.login.callback.handler.class", "io.github.nniikkoollaaii.kafka.sasl.oauthbearer.workload_identity.WorkloadIdentityLoginCallbackHandler");

        props.put("bearer.auth.logical.cluster", "123456");
        props.put("bearer.auth.credentials.source", "CUSTOM");
        props.put("bearer.auth.custom.provider.class", "io.github.nniikkoollaaii.kafka.bearerauth.workload_identity.WorkloadIdentityBearerAuthCredentialProvider");
        

        // Create the Kafka consumer
        Consumer<String, GenericRecord> consumer = new KafkaConsumer<>(props);

        // Subscribe to the Kafka topic
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));

        // Continuously poll for new records
        while (true) {
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(100));

            // Process each record
            records.forEach(record -> {
                GenericRecord event = record.value();
                System.out.println("Received event: " + event.toString());
            });

            // Exit the consumer loop after processing the first record
            if (!records.isEmpty()) {
                break;
            }
        }
        
        consumer.close();
    }
}

