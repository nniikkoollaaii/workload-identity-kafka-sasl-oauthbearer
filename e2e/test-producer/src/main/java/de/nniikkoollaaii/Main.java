import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static final String TOPIC_NAME = "de.nniikkoollaaii.topic";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    public static void main(String[] args) {
        // Kafka producer configuration
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.oauthbearer.token.endpoint.url", "https://login.microsoftonline.com/common/oauth2/v2.0/token");
        props.put("sasl.oauthbearer.scope.claim.name", "scp");
        
        props.put("sasl.login.callback.handler.class", "org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler");
        //props.put("sasl.login.callback.handler.class", "de.nniikkoollaaii.WorkloadIdentityLoginCallbackHandler");

        props.put("sasl.mechanism", "OAUTHBEARER");
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId='<client ID>' scope='<Requested Scope>' clientSecret='<Client Secret>';");
        //props.put("sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");

        // Create the Kafka producer
        Producer<String, String> producer = new KafkaProducer<>(props);

        // Create a timer task to send events every 5 seconds
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // Generate a random event
                String event = generateEvent();

                // Publish the event to the Kafka topic
                producer.send(new ProducerRecord<>(TOPIC_NAME, event));
                System.out.println("Published event: " + event);
            }
        };

        // Schedule the timer task to run every 5 seconds
        Timer timer = new Timer();
        timer.schedule(task, 0, 5000);

        // Keep the application running for some time
        try {
            Thread.sleep(60000); // Run for 60 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Clean up and close the Kafka producer
        producer.close();
        timer.cancel();
    }

    private static String generateEvent() {
        // Implement your event generation logic here
        // For simplicity, let's just return a timestamp
        return Long.toString(System.currentTimeMillis());
    }
}
