package io.github.nniikkoollaaii;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static final String TOPIC_NAME = "nniikkoollaaii.topic";

    public static void main(String[] args) {

        // Kafka producer configuration
        Properties props = new Properties();
        props.put("bootstrap.servers", System.getenv("BOOTSTRAP_SERVERS"));
        props.put("schema.registry.url", System.getenv("SCHEMA_REGISTRY_URL"));
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", KafkaAvroSerializer.class);
        
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

        props.put("bearer.auth.logical.cluster", "123456");
        props.put("bearer.auth.credentials.source", "WORKLOAD_IDENTITY_OAUTHBEARER");
        props.put("bearer.auth.custom.provider.class", "io.github.nniikkoollaaii.kafka.bearerauth.workload_identity.WorkloadIdentityBearerAuthCredentialProvider");


        // Create the Kafka producer
        String avroSchemaString = "{\"type\": \"record\", \"name\": \"ExampleRecord\", \"fields\": [{\"name\": \"content\", \"type\": \"string\"}]}";
        Schema.Parser parser = new Schema.Parser();
        Schema avroSchema = parser.parse(avroSchemaString);

        Producer<String, GenericRecord> producer = new KafkaProducer<>(props);


        String event = System.getenv("MESSAGE_CONTENT");

        // Publish the event to the Kafka topic
        String key = "1";
        GenericRecord record = new GenericData.Record(avroSchema);
        record.put("content", event);

        System.out.printf("Producing record: %s\t%s%n", key, record);
        producer.send(new ProducerRecord<>(TOPIC_NAME, key, record), (m, e) -> {
        if (e != null) {
            e.printStackTrace();
        } else {
            System.out.printf("Produced record to topic %s partition [%d] @ offset %d%n", m.topic(), m.partition(), m.offset());
        }
        });


        producer.flush();
        System.out.println("Published event: " + event);

        // Clean up and close the Kafka producer
        producer.close();
    }
}
