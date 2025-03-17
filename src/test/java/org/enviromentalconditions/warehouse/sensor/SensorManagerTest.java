package org.enviromentalconditions.warehouse.sensor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SensorManagerTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("SensorManagerTest");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSendValidSensorMessage() {
        new TestKit(system) {
            {
                final TestProbe sensorProbe = new TestProbe(system);

                final ActorRef sensorManager = system
                        .actorOf(Props.create(SensorManager.class, () -> new SensorManager(SensorType.TEMPERATURE) {
                            @Override
                            protected ActorRef createSensorActor(String sensorId, SensorType type) {
                                // Return our test probe instead of creating a real SensorActor
                                return sensorProbe.ref();
                            }
                        }));
                long timestampMillis = System.currentTimeMillis();
                sensorManager.tell("sensor_id=t1; value=80; timestamp=" + timestampMillis, getRef());

                SensorReading reading = sensorProbe.expectMsgClass(SensorReading.class);
                assertThat(reading.sensorId()).isEqualTo("t1");
                assertThat(reading.sensorType()).isEqualTo(SensorType.TEMPERATURE);
                assertThat(reading.value()).isEqualTo(80f);
                assertThat(reading.timestamp()).isEqualTo(timestampMillis);
            }
        };
    }

    @Test
    public void testInvalidFormatMessage() {
        final TestActorRef<SensorManager> testRef = TestActorRef.create(system,
                Props.create(SensorManager.class, SensorType.TEMPERATURE));
        String invalidFormatMessage = "some-unparseable-format";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid sensor value: " + invalidFormatMessage);

        testRef.receive(invalidFormatMessage);
    }

    @Test
    public void testSensorTerminated() throws IllegalAccessException {
        new TestKit(system) {
            {
                final TestProbe sensorProbe = new TestProbe(system);

                final ActorRef sensorManager = system
                        .actorOf(Props.create(SensorManager.class, () -> new SensorManager(SensorType.TEMPERATURE) {
                            @Override
                            protected ActorRef createSensorActor(String sensorId, SensorType type) {
                                // Return our test probe instead of creating a real SensorActor
                                return sensorProbe.ref();
                            }
                        }));
                long timestampMillis = System.currentTimeMillis();
                sensorManager.tell("sensor_id=t1; value=80; timestamp=" + timestampMillis, getRef());

                SensorManager.SensorTerminated terminated = new SensorManager.SensorTerminated(sensorProbe.ref(), "t1");

                sensorManager.tell(terminated, getRef());
                expectNoMessage();
            }
        };
    }

    @Test
    public void testSensorActorForMultipleCalls() {
        new TestKit(system) {
            {
                final TestProbe sensorProbeT1 = new TestProbe(system);
                final TestProbe sensorProbeT2 = new TestProbe(system);

                final ActorRef sensorManager = system
                        .actorOf(Props.create(SensorManager.class, () -> new SensorManager(SensorType.TEMPERATURE) {
                            @Override
                            protected ActorRef createSensorActor(String sensorId, SensorType type) {
                                if ("t1".equals(sensorId)) {
                                    return sensorProbeT1.ref();
                                } else if ("t2".equals(sensorId)) {
                                    return sensorProbeT2.ref();
                                }
                                throw new IllegalArgumentException("Invalid sensor ID: " + sensorId);
                            }
                        }));

                String messageOne = "sensor_id=t1; value=23.5; timestamp=1617283945123";
                String messageTwo = "sensor_id=t1; value=24.0; timestamp=1617283945456";

                sensorManager.tell(messageOne, getRef());
                sensorManager.tell(messageTwo, getRef());

                String message3 = "sensor_id=t2; value=18.2; timestamp=1617283945789";
                sensorManager.tell(message3, getRef());

                expectNoMessage();

                sensorProbeT1.expectMsgClass(SensorReading.class);
                sensorProbeT1.expectMsgClass(SensorReading.class);
                sensorProbeT1.expectNoMessage(new FiniteDuration(100, TimeUnit.MILLISECONDS));

                sensorProbeT2.expectMsgClass(SensorReading.class);
                sensorProbeT2.expectNoMessage(new FiniteDuration(100, TimeUnit.MILLISECONDS));
            }
        };
    }
}
