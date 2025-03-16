package org.enviromentalconditions.warehouse.sensor;

import java.io.Serializable;

public record SensorReading(String sensorId, long timestamp, SensorType sensorType, Float value)
        implements Serializable {

    private static final long serialVersionUID = 1L;

}
