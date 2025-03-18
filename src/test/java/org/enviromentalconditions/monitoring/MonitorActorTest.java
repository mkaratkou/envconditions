package org.enviromentalconditions.monitoring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.alpakka.mqtt.MqttMessage;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import org.enviromentalconditions.warehouse.sensor.SensorReading;
import org.enviromentalconditions.warehouse.sensor.SensorType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import utils.MqttUtils;

import java.util.concurrent.TimeUnit;

public class MonitorActorTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testHandleMessageBelowThreshold() {
        new TestKit(system) {
            {
                final TestProbe alertProbe = new TestProbe(system);

                // Create the actor with our test probe factory
                ActorRef monitorActor = system
                        .actorOf(Props.create(MonitorActor.class, () -> new MonitorActor(SensorType.TEMPERATURE, 30.0f,
                                (sensorType, sensorId) -> alertProbe.ref())));

                long timestamp = System.currentTimeMillis();
                // Create a sensor reading above the threshold
                SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 1.0f, timestamp);

                // Create a real MqttMessage with the payload
                MqttMessage mqttMessage = MqttMessage.create("topic", MqttUtils.createPayload(reading));

                // Send the message to the actor
                monitorActor.tell(mqttMessage, getRef());

                // Verify that the AlertSenderActor received the reading
                alertProbe.expectNoMessage();
            }
        };
    }

    @Test
    public void testHandleMessageAboveThreshold() {
        new TestKit(system) {
            {
                final TestProbe alertProbe = new TestProbe(system);

                // Create the actor with our test probe factory
                ActorRef monitorActor = system
                        .actorOf(Props.create(MonitorActor.class, () -> new MonitorActor(SensorType.TEMPERATURE, 30.0f,
                                (sensorType, sensorId) -> alertProbe.ref())));

                long timestamp = System.currentTimeMillis();
                // Create a sensor reading above the threshold
                SensorReading reading = new SensorReading("t1", SensorType.TEMPERATURE, 35.0f, timestamp);

                // Create a real MqttMessage with the payload
                MqttMessage mqttMessage = MqttMessage.create("topic", MqttUtils.createPayload(reading));

                // Send the message to the actor
                monitorActor.tell(mqttMessage, getRef());

                // Verify that the AlertSenderActor received the reading
                alertProbe.expectMsg(Duration.create(3, TimeUnit.SECONDS), reading);
            }
        };

    }
}
