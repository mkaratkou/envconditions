package org.enviromentalconditions.monitoring;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.enviromentalconditions.warehouse.sensor.SensorReading;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AlertSenderActor extends AbstractActor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final ZoneId ZONE_ID = ZoneId.of("America/New_York");

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(AlertSenderActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(SensorReading.class, this::handleMessage).build();
    }

    private void handleMessage(SensorReading sensorReading) {
        log.warning(
                "SensorId={} of type='{}' has received reading of value={} which exceeded the threshold; reading timestamp={}. ",
                sensorReading.sensorId(), sensorReading.sensorType().getValue(), sensorReading.value(),
                Instant.ofEpochMilli(sensorReading.timestamp()).atZone(ZONE_ID).format(FORMATTER));
    }

}
