package org.enviromentalconditions.warehouse.listener;

import akka.actor.AbstractActor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.io.Udp;
import akka.io.UdpMessage;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class UdpListenerActor extends AbstractActor {
    final ActorRef delegateRef;

    public static Props props(String host, int port, ActorRef delegateRef) {
        return Props.create(UdpListenerActor.class, host, port, delegateRef);
    }

    public UdpListenerActor(String host, int port, ActorRef delegateRef) {
        this.delegateRef = delegateRef;

        // request creation of a bound listen socket
        final ActorRef mgr = Udp.get(getContext().getSystem()).getManager();

        mgr.tell(UdpMessage.bind(getSelf(), new InetSocketAddress(host, port)), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Udp.Bound.class, bound -> getContext().become(ready(getSender()))).build();
    }

    private Receive ready(final ActorRef socket) {
        return receiveBuilder().match(Udp.Received.class, r -> {
            socket.tell(UdpMessage.send(r.data(), r.sender()), getSelf());
            String utf8String = r.data().decodeString(StandardCharsets.UTF_8); // parse data etc., e.g. using
                                                                               // PipelineStage
            String message = String.format("%s; timestamp=%d", utf8String, System.currentTimeMillis());
            delegateRef.tell(message, getSelf());
        }).matchEquals(UdpMessage.unbind(), message -> socket.tell(message, getSelf()))
                .match(Udp.Unbound.class, message -> {
                    ActorContext context = getContext();
                    context.stop(delegateRef);
                    context.stop(getSelf());
                } ).build();
    }
}
