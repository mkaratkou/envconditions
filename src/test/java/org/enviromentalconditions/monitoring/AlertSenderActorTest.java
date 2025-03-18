package org.enviromentalconditions.monitoring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.EventFilter;
import akka.testkit.javadsl.TestKit;
import org.enviromentalconditions.warehouse.sensor.SensorReading;
import org.enviromentalconditions.warehouse.sensor.SensorType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.typesafe.config.ConfigFactory;
import akka.actor.testkit.typed.javadsl.LogCapturing;
import org.junit.Rule;

public class AlertSenderActorTest {

    private static ActorSystem system;

    @Rule
    public final LogCapturing logCapturing = new LogCapturing();

    @BeforeClass
    public static void setup() {
        // Configure the test system to use TestEventListener for logging
        system = ActorSystem.create("AlertSenderActorTest",
                ConfigFactory.parseString("akka.loggers = [\"akka.testkit.TestEventListener\"]"));
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testAlertSenderLogsWarningOnSensorReading() {
        new TestKit(system) {
            {
                // Create the AlertSenderActor
                final Props props = AlertSenderActor.props();
                final ActorRef alertSender = system.actorOf(props, "alertSender");

                // Create a test SensorReading
                final String sensorId = "t1";
                final SensorType sensorType = SensorType.TEMPERATURE;
                final float value = 35.1f;
                final long timestamp = System.currentTimeMillis();

                SensorReading reading = new SensorReading(sensorId, sensorType, value, timestamp);

                // Use EventFilter to verify that a warning is logged
                EventFilter.warning(null, // message - use null to rely on pattern matching instead
                        alertSender.path().toString(), null, // start - use null for default behavior
                        ".*SensorId=" + sensorId + ".*type='" + sensorType.getValue() + "'.*value=" + value + ".*", 1)
                        .intercept(() -> {
                            alertSender.tell(reading, getRef());
                            return null;
                        }, system);
            }
        };
    }
}
