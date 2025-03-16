package org.enviromentalconditions.warehouse.sensor;

import akka.Done;
import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.mqtt.MqttConnectionSettings;
import akka.stream.alpakka.mqtt.MqttMessage;
import akka.stream.alpakka.mqtt.MqttQoS;
import akka.stream.alpakka.mqtt.javadsl.MqttSink;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.enviromentalconditions.warehouse.ConfigPath;
import utils.MqttUtils;

import java.util.Collections;
import java.util.concurrent.CompletionStage;

import static org.enviromentalconditions.AppNames.WAREHOUSE_SERVICE;

public class SensorActor extends AbstractActor {

    private final String sensorId;
    private final SensorType sensorType;
//    private SensorReading sensorReading; - move to state if we'd like to use persistence?
    private MqttConnectionSettings connectionSettings;
    private String mqttTopic;
    private final ActorMaterializer materializer;
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(String sensorId, SensorType sensorType) {
        return Props.create(SensorActor.class, sensorId, sensorType);
    }

    public SensorActor(String sensorId, SensorType sensorType) {
        this.sensorType = sensorType;
        this.sensorId = sensorId;
        initMqttSettings();
        materializer = ActorMaterializer.create(getContext());
    }

    private void initMqttSettings() {
        Config config = ConfigFactory.load(WAREHOUSE_SERVICE);
        this.mqttTopic = config.getString(ConfigPath.MQTT_TOPIC.forSensorType(sensorType));
        String brokerAddress = config.getString(ConfigPath.BROKER_ADDRESS.forSensorType(sensorType));
        String clientId = String.format("%s-sensor-subscriber-client", sensorType.getValue());
        connectionSettings =
                MqttConnectionSettings.create(brokerAddress,
                                clientId,
                                new MemoryPersistence()
                        )
                        .withCleanSession(true);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SensorReading.class, reading -> {
                    log.debug("Sensor >>> Publishing to MQTT Topic={} for sensorId={}, value={}", mqttTopic, sensorId, reading.value());
//                    this.sensorReading = reading;

                    MqttMessage mqttMessage = MqttMessage.create(mqttTopic, MqttUtils.createPayload(reading))
                            .withQos(MqttQoS.atLeastOnce())
                            .withRetained(true);

                    Sink<MqttMessage, CompletionStage<Done>> mqttSink =
                            MqttSink.create(connectionSettings, MqttQoS.atLeastOnce());
                    Source.from(Collections.singletonList(mqttMessage)).runWith(mqttSink, materializer);
                }).build();
    }

}
