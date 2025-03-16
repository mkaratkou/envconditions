package org.enviromentalconditions.warehouse.sensor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.typed.PostStop;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensorManager extends AbstractActor {

    private static final String SENSOR_ID_GROUP = "sensorid";
    private static final String VALUE_GROUP = "value";
    private static final String TIMESTAMP_GROUP = "timestamp";

    private static final Pattern SENSOR_VALUE_PATTERN = Pattern.compile(
            "sensor_id=(?<sensorid>.*); value=(?<value>[+-]?([0-9]*[.])?[0-9]+); timestamp=(?<timestamp>\\d*)$");

    private final Map<String, ActorRef> sensorIdToSensorActorRefMap = new HashMap<>();
    private final SensorType sensorType;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public SensorManager(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public interface Command {
    }

    public static class SensorTerminated implements Command {
        private final ActorRef sensorActorRef;
        private final String sensorId;

        public SensorTerminated(ActorRef sensorActorRef, String sensorId) {
            this.sensorActorRef = sensorActorRef;
            this.sensorId = sensorId;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(String.class, this::sendMessage).match(SensorTerminated.class, terminated -> {
            log.debug("Ref of Sensor actor={} terminated and removed from SensorManager. SensorId={}",
                    terminated.sensorActorRef, terminated.sensorId);
            sensorIdToSensorActorRefMap.remove(terminated.sensorId);
        }).match(PostStop.class, signal -> postStop()).build();
    }

    private void sendMessage(String dataMessage) {
        Matcher valueMatcher = SENSOR_VALUE_PATTERN.matcher(dataMessage);
        if (!valueMatcher.matches()) {
            throw new IllegalArgumentException("Invalid sensor value: " + dataMessage);
        }
        String sensorId = valueMatcher.group(SENSOR_ID_GROUP);
        String value = valueMatcher.group(VALUE_GROUP);
        long timestamp = Long.parseLong(valueMatcher.group(TIMESTAMP_GROUP));
        SensorReading reading = new SensorReading(sensorId, timestamp, sensorType, Float.valueOf(value));

        ActorRef sensorRef = sensorIdToSensorActorRefMap.computeIfAbsent(sensorId, id -> {
            ActorRef r = getContext().actorOf(SensorActor.props(sensorId, sensorType),
                    String.format("%Sensor-%s", sensorType.getValue(), sensorId));
            getContext().watchWith(r, new SensorTerminated(r, sensorId));
            return r;
        });
        sensorRef.tell(reading, getSelf());
    }

}
