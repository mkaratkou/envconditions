package org.enviromentalconditions.warehouse;

import org.enviromentalconditions.warehouse.sensor.SensorType;

import java.text.MessageFormat;

public enum ConfigPath {

    UDP_HOST("warehouse-service.{0}.udp.host"), UDP_PORT("warehouse-service.{0}.udp.port"),
    MQTT_TOPIC("warehouse-service.{0}.mqtt.topic"), BROKER_ADDRESS("warehouse-service.{0}.mqtt.brokerAddress");

    private final String pattern;

    ConfigPath(String pattern) {
        this.pattern = pattern;
    }

    public String forSensorType(SensorType sensorType) {
        return MessageFormat.format(this.pattern, sensorType.getValue());
    }

}
