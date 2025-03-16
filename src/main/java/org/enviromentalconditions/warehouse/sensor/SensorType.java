package org.enviromentalconditions.warehouse.sensor;

public enum SensorType {
    TEMPERATURE("temperature"), HUMIDITY("humidity"),;

    private final String value;

    SensorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
