package org.enviromentalconditions.warehouse.sensor;

import java.io.Serializable;

public record SensorReading(String sensorId, SensorType sensorType, Float value, long timestamp)
        implements Serializable {

    private static final long serialVersionUID = 1L;

}
