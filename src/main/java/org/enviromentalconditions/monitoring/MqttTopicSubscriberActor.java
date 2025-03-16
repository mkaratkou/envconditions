package org.enviromentalconditions.monitoring;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.mqtt.MqttConnectionSettings;
import akka.stream.alpakka.mqtt.MqttMessage;
import akka.stream.alpakka.mqtt.MqttQoS;
import akka.stream.alpakka.mqtt.MqttSubscriptions;
import akka.stream.alpakka.mqtt.javadsl.MqttSource;
import akka.stream.javadsl.Sink;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.enviromentalconditions.warehouse.sensor.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.enviromentalconditions.AppNames.MONITORING_SERVICE;

public class MqttTopicSubscriberActor extends AbstractActor {

    private static final int BUFFER_SIZE = 100;
    public static final int PARALLELISM = 20;

    private static final Logger log = LoggerFactory.getLogger(MqttTopicSubscriberActor.class);

    private final SensorType sensorType;

    private final String subscriberClientId;
    private final String mqttTopic;
    private final String brokerAddress;

    public static Props props(SensorType sensorType) {
        return Props.create(MqttTopicSubscriberActor.class, sensorType);
    }

    public MqttTopicSubscriberActor(SensorType sensorType) {
        this.sensorType = sensorType;
        Config config = ConfigFactory.load(MONITORING_SERVICE);
        this.subscriberClientId = String.format("%sMonitorClientId", sensorType.getValue());
        this.mqttTopic = config.getString(ConfigPath.MQTT_TOPIC.forSensorType(sensorType));
        this.brokerAddress = config.getString(ConfigPath.BROKER_ADDRESS.forSensorType(sensorType));
    }

    @Override
    public void preStart() {
        subscribe();
    }

    private void subscribe() {
        Materializer materializer = ActorMaterializer.create(getContext());
        Config config = ConfigFactory.load(MONITORING_SERVICE);
        float threshold = Float.parseFloat(config.getString(ConfigPath.THRESHOLD.forSensorType(sensorType)));

        // Create a router actor to handle messages
        ActorRef alertManager = getContext().actorOf(MonitorActor.props(sensorType, threshold),
                String.format("%sMonitor", sensorType.getValue()));

        // Configure MQTT connection using only Alpakka's API
        MqttConnectionSettings connectionSettings = MqttConnectionSettings
                .create(this.brokerAddress, this.subscriberClientId, new MemoryPersistence()).withCleanSession(false);

        // Configure subscriptions with at-least-once QoS
        MqttSubscriptions subscriptions = MqttSubscriptions.create(this.mqttTopic, MqttQoS.atLeastOnce());

        // Use Alpakka's MQTT Source with acknowledgment
        MqttSource.atLeastOnce(connectionSettings, subscriptions, BUFFER_SIZE).mapAsync(PARALLELISM, messageWithAck -> {
            // Send to actor and get future result
            CompletionStage<Done> processingDone = askActor(alertManager, messageWithAck.message());

            // When processing completes, acknowledge the message
            return processingDone
                    .thenCompose(done -> messageWithAck.ack().thenApply(ackDone -> messageWithAck.message()));
        }).runWith(Sink.foreach(m -> log.debug("Processed and acknowledged for topic={}.", m.topic())), materializer);
    }

    private static CompletionStage<Done> askActor(ActorRef actor, MqttMessage message) {
        actor.tell(message, ActorRef.noSender());
        CompletableFuture<Done> future = new CompletableFuture<>();
        future.complete(Done.getInstance());
        return future;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }

}
