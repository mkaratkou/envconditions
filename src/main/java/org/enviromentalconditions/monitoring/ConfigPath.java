package org.enviromentalconditions.monitoring;

import org.enviromentalconditions.warehouse.sensor.SensorType;

import java.text.MessageFormat;

public enum ConfigPath {

    MQTT_TOPIC("monitoring-service.{0}.mqtt.topic"),
    BROKER_ADDRESS("monitoring-service.{0}.mqtt.brokerAddress"),
    THRESHOLD("monitoring-service.{0}.threshold");

    private final String pattern;

    ConfigPath(String pattern) {
        this.pattern = pattern;
    }

    public String forSensorType(SensorType sensorType) {
        return MessageFormat.format(this.pattern, sensorType.getValue());
    }
}
