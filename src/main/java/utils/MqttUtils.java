package utils;

import akka.stream.alpakka.mqtt.MqttMessage;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public final class MqttUtils {

    private static final Logger logger = LoggerFactory.getLogger(MqttUtils.class);

    public static ByteString createPayload(Object message) {
        logger.trace("createPayload() called for message={}", message);
        byte[] byteArray = null;
        ByteArrayOutputStream byteArrayOutputStream;
        ObjectOutputStream objectOutputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(message);
            byteArray = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            logger.error("Error creating payload", e);
        } finally {
            try {
                objectOutputStream.close();
            } catch (IOException e) {
                logger.error("Error closing streams", e);
            }
        }
        return ByteString.fromArray(byteArray);
    }

    @SuppressWarnings("unchecked")
    public static <T> T extractMessagePayload(MqttMessage mqttMessage, Class<T> expectedClass) {
        logger.trace("getPayloadFromMqttMessage() for message={} of class={}", mqttMessage, expectedClass);
        ObjectInputStream objectInputStream = null;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mqttMessage.payload().toArray());
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object o = objectInputStream.readObject();
            if (o == null) {
                return null;
            }
            if (expectedClass.isAssignableFrom(o.getClass())) {
                return (T) o;
            }
            throw new IllegalArgumentException(String.format("Invalid payload class=%s, but expected=%s",
                    o.getClass().getName(), expectedClass.getName()));
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error getting payload", e);
        } finally {
            try {
                objectInputStream.close();
            } catch (IOException e) {
                logger.error("Error closing streams", e);
            }
        }
        return null;
    }

    private MqttUtils() {
        // prevent instantiation
    }

}
