package org.enviromentalconditions.monitoring;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.alpakka.mqtt.MqttMessage;
import org.enviromentalconditions.warehouse.sensor.SensorReading;
import org.enviromentalconditions.warehouse.sensor.SensorType;
import utils.MqttUtils;

import java.util.HashMap;
import java.util.Map;

public class MonitorActor extends AbstractActor {

    private final SensorType sensorType;
    private final float threshold;
    private final Map<String, ActorRef> sensorIdToAlertSenderRefMap = new HashMap<>();

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(SensorType sensorType, float threshold) {
        return Props.create(MonitorActor.class, sensorType, threshold);
    }

    public MonitorActor(SensorType sensorType, float threshold) {
        this.sensorType = sensorType;
        this.threshold = threshold;
    }

    public interface Command {
    }

    private static class SenderTerminated implements Command {
        public final ActorRef alertSenderRef;
        public final String sensorId;

        private SenderTerminated(ActorRef alertSenderRef, String sensorId) {
            this.alertSenderRef = alertSenderRef;
            this.sensorId = sensorId;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(MqttMessage.class, this::handleMessage)
                .match(SenderTerminated.class, this::onSenderTerminated).build();
    }

    private void onSenderTerminated(SenderTerminated senderTerminated) {
        log.debug("Sender={} with sensorId={} has been terminated", senderTerminated.alertSenderRef,
                senderTerminated.sensorId);
        sensorIdToAlertSenderRefMap.remove(senderTerminated.sensorId);
    }

    private void handleMessage(MqttMessage mqttMessage) {
        SensorReading reading = MqttUtils.extractMessagePayload(mqttMessage, SensorReading.class);

        log.debug("Values received by {} sensorId={}, sensorValue={}", sensorType.getValue(), reading.sensorId(),
                reading.value());

        if (reading.value() > threshold) {
            log.debug("Value={} has exceeded threshold={}", reading.value(), threshold);
            ActorRef actorRef = sensorIdToAlertSenderRefMap.computeIfAbsent(reading.sensorId(), sensorId -> {
                log.debug("Finding actor for sensorId={} ", sensorId);
                ActorRef r = getContext().actorOf(AlertSenderActor.props(),
                        String.format("%sAlertSenderActor-%s", sensorType.getValue(), sensorId));
                getContext().watchWith(r, new SenderTerminated(r, sensorId));
                return r;
            });
            log.debug("Sending message to Alert Actor={}, value={}", actorRef, reading.value());
            actorRef.tell(reading, getSelf());
        }
    }
}
