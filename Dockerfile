# the base image
FROM amazoncorretto:21
# the JAR file path
ARG JAR_FILE=target/*-allinone.jar

# Copy the JAR file from the build context into the Docker image
COPY ${JAR_FILE} app.jar

CMD apt-get update -y
CMD cp

# Set the default command to run the Java application
CMD ["java", "-Xmx1048M", "-jar", "./app.jar"]
#ENTRYPOINT ["java", "-Xmx1024M", "-DAPP_CONFIG=warehouse-service.conf", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]
