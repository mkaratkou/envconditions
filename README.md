
Build applications locally:
```shell
mvn clean package
docker build . -t mkaratkou/envconditions
```

Run applications:
```shell
docker-compose up -d
```

To stop the apps run:
```shell
 docker-compose down
```
To send data to the app run:
```shell
echo -n "sensor_id=t1; value=80" | nc -u -c -w1 localhost 3344
echo -n "sensor_id=h1; value=81" | nc -u -c -w1 localhost 3355
```
