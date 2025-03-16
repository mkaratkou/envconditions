package org.enviromentalconditions;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.enviromentalconditions.monitoring.MqttTopicSubscriberActor;
import org.enviromentalconditions.warehouse.listener.UdpListenerActor;
import org.enviromentalconditions.warehouse.sensor.SensorManager;
import org.enviromentalconditions.warehouse.sensor.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.enviromentalconditions.AppNames.MONITORING_SERVICE;
import static org.enviromentalconditions.AppNames.WAREHOUSE_SERVICE;
import static org.enviromentalconditions.warehouse.ConfigPath.UDP_HOST;
import static org.enviromentalconditions.warehouse.ConfigPath.UDP_PORT;

public class AppServiceMain {

    private static final Logger logger = LoggerFactory.getLogger(AppServiceMain.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            logger.error("Invalid number of arguments. Expected 1 but got {}", args.length);
            System.exit(1);
        }
        if ("warehousesvc".equals(args[0])) {
            final Config config = ConfigFactory.load(WAREHOUSE_SERVICE);
            ActorSystem system = ActorSystem.create(WAREHOUSE_SERVICE, config);
            try {
                initWarehouseService(system, config);
            } catch (Exception e) {
                logger.error("Terminating due to initialization failure of warehouse service.", e);
                system.terminate();
            }
        } else if ("monitoringsvc".equals(args[0])) {
            final Config config = ConfigFactory.load(MONITORING_SERVICE);
            ActorSystem system = ActorSystem.create(MONITORING_SERVICE, config);
            try {
                initMonitoringService(system);
            } catch (Exception e) {
                logger.error("Terminating due to initialization failure of monitoring service.", e);
                system.terminate();
            }
        } else {
            throw new IllegalArgumentException(
                    "Invalid service name=" + args[0] + "; expected: {warehousesvc|monitoringsvc}");
        }
    }

    private static void initWarehouseService(ActorSystem system, Config config) throws UnknownHostException {
        createSensorListenerAndManager(system, config, SensorType.TEMPERATURE);
        createSensorListenerAndManager(system, config, SensorType.HUMIDITY);
    }

    private static void createSensorListenerAndManager(ActorSystem system, Config config, SensorType sensorType)
            throws UnknownHostException {
        ActorRef sensorManagerRef = system.actorOf(Props.create(SensorManager.class, sensorType),
                String.format("%sSensorManager", sensorType.getValue()));

        String udpHostName = config.getString(UDP_HOST.forSensorType(sensorType));
        String host = InetAddress.getByName(udpHostName).getHostAddress();
        system.actorOf(
                UdpListenerActor.props(host, config.getInt(UDP_PORT.forSensorType(sensorType)), sensorManagerRef),
                String.format("%sUdpListener", sensorType.getValue()));
    }

    private static void initMonitoringService(ActorSystem system) {
        createTopicSubscriber(system, SensorType.TEMPERATURE);
        createTopicSubscriber(system, SensorType.HUMIDITY);
    }

    private static ActorRef createTopicSubscriber(ActorSystem system, SensorType sensorType) {
        ActorRef subscriber = system.actorOf(MqttTopicSubscriberActor.props(sensorType),
                String.format("%sMqttTopicSubscriber", sensorType.getValue()));
        return subscriber;
    }

}
