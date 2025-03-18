package org.enviromentalconditions.warehouse.listener;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.testkit.javadsl.TestKit;
import akka.util.ByteString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class UdpListenerActorTest {

    private static final int UDP_PORT = 3344;
    private static final String UDP_HOST = "localhost";

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("UdpListenerActorTest");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testMessageReceived() {
        new TestKit(system) {
            {
                final TestKit delegateProbe = new TestKit(system);
                final TestKit mockSocket = new TestKit(system);

                final ActorRef udpListener = system
                        .actorOf(UdpListenerActor.props(UDP_HOST, UDP_PORT, delegateProbe.getRef()));

                udpListener.tell(new Udp.Bound(new InetSocketAddress(UDP_HOST, UDP_PORT)), mockSocket.getRef());

                // prepare message and the data
                String sensorReading = "sensor_id=t1; value=80";
                ByteString data = ByteString.fromString(sensorReading, StandardCharsets.UTF_8);
                InetSocketAddress sender = new InetSocketAddress("remote-host", UDP_PORT);

                udpListener.tell(new Udp.Received(data, sender), mockSocket.getRef());

                mockSocket.expectMsgClass(Udp.Send.class);
                String receivedMessage = delegateProbe.expectMsgClass(String.class);
                assertThat(receivedMessage).startsWith(sensorReading);
                assertThat(receivedMessage).matches("(.*)timestamp=(\\d+)$");
            }
        };
    }

    @Test
    public void testUnbound() {
        new TestKit(system) {
            {
                final TestKit delegateProbe = new TestKit(system);
                final ActorRef delegateRef = delegateProbe.getRef();
                watch(delegateRef);

                final ActorRef udpListener = system.actorOf(UdpListenerActor.props(UDP_HOST, UDP_PORT, delegateRef));
                watch(udpListener);
                final TestKit mockSocket = new TestKit(system);

                udpListener.tell(new Udp.Bound(new InetSocketAddress(UDP_HOST, UDP_PORT)), mockSocket.getRef());
                udpListener.tell(UdpMessage.unbind(), getRef());
                udpListener.tell(new Udp.Unbound() {
                }, mockSocket.getRef());

                expectTerminated(Duration.ofSeconds(3), delegateRef);
                expectTerminated(Duration.ofSeconds(3), udpListener);
            }
        };
    }
}
