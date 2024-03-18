# XV Cloud Native Workshop

Useful links:

* DevSpaces
** https://devspaces.apps.oscadetest.bank.ad.bxs.com
* Openshift Documentaion
** https://access.redhat.com/documentation/en-us/openshift_container_platform/4.14
* Kubernetes documentation
** https://Kubernetes.io


## Lab 0

This lab is meant to setup the Openshift cluster, it should only be executed by the person running the workshop.

## Lab 0.1 Provide AMQ Streams Operator and Kafka Cluster

Install Operator:
* AMQ Streams
* Version: 2.5.1-2 provided by RH

Create the following Kafka cluster:

```
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka-cluster
spec:
  kafka:
    version: 3.5.0
    replicas: 1
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 1
      transaction.state.log.replication.factor: 1
      transaction.state.log.min.isr: 1
      default.replication.factor: 1
      min.insync.replicas: 1
      inter.broker.protocol.version: "3.5"
    storage:
      type: ephemeral
  zookeeper:
    replicas: 1
    storage:
      type: ephemeral
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

Provide an Openshift Serverless Operator:

* Openshift serverless operator
* Version 1.32.0

Install knative serving

```
oc new-project knative-serving
```

Create the serving configuration:

```
apiVersion: operator.knative.dev/v1beta1
kind: KnativeServing
metadata:
    name: knative-serving
    namespace: knative-serving
```

Check knative serving status and wait until everything is reported as `true`

```
oc get knativeserving.operator.knative.dev/knative-serving -n knative-serving --template='{{range .status.conditions}}{{printf "%s=%s\n" .type .status}}{{end}}'
```

## Lab 1

### Lab 1.1 Deploying Order Service

The order service requires a postgresql database to store order data.

In order to deploy the database, it is possible to use the provided openshift templates.

It is not required to configured persistence so the template PostgreSQL (ephemeral can be used).

Configure a PostgreSQL ephemeral database.

To do this, go to the Developer view and click "Add" to search the catalog and search for `PostgreSQL (Ephemeral)`

Configure the template with the following values:

* Databse Service Name: orders
* PostgreSQL Username: quarkus
* PostgreSQL Password: quarkus
* PostgreSQL Database Name: orders

Click `Create` to have the service created.

The Openshift template will deploy an ephemeral PostgreSQL database with the provided configuration.

Next step is to deploy the order-service.

To do that, create a build configuration for the service:

```
oc new-build --name order-service --binary=true
```

The provided `Dockerfile` is not created in the default directory, due to this it is required to modify the path specified in the build configuration.

Modify the created `BuildConfiguration` resource to add:

```
strategy:
  type: Docker
  dockerStrategy:
    dockerfilePath: src/main/docker/Dockerfile.jvm
```

Compile the service locally before running the OCP build. In this case, the dockerfile requires a compiled application:

```
mvn clean package
```

Start the build from the `order-service`:

```
oc start-build order-service --from-dir=. --follow
```

And deploy it:

```
oc new-app --name=order-service --image-stream=order-service:latest
```

This will provide a `Deployment` and a `Service` for the `order-service`. 

Make sure the deployment is blue and that the service is running by executing a curl:

```
curl http://localhost:8080/entity/orders
```

## Lab 1.2 Deploy UI

To interact with the services deployed, a simple NGINX application is implemented.

To deploy it, it is only required to create a `BuildConfiguration` and an `ImageStream`:

```
oc new-build --name ui-service --binary=true
```

So it is possible to build the UI image from the `ui-service` folder:

```
oc start-build ui-service --from-dir=. --follow
```

Create a new application from the created image:

```
oc new-app --name=ui-service --image-stream=ui-service:latest
```

Expose the application to consumers outside the cluster:

```
oc expose svc ui-service
```

A route should be created, to check its hostname run:

```
oc get routes
```

Validate that it is possible to access the application:

```
http://<my-route>/index.htlm
```


## Lab 1.3 Deploy Price Service


The price service is similar to the order service and it requires a postgresql database to store order data.

In order to deploy the database, it is possible to use the provided openshift templates.

It is not required to configured persistence so the template PostgreSQL (ephemeral can be used).

Configure a PostgreSQL ephemeral database.

To do this, go to the Developer view and click "Add" to search the catalog and search for `PostgreSQL (Ephemeral)`

Configure the template with the following values:

* Databse Service Name: prices
* PostgreSQL Username: quarkus
* PostgreSQL Password: quarkus
* PostgreSQL Database Name: prices

Click `Create` to have the service created.

The Openshift template will deploy an ephemeral PostgreSQL database with the provided configuration.

Next step is to deploy the price-service.

To do that, create a build configuration for the service:

```
oc new-build --name price-service --binary=true
```

The provided `Dockerfile` is not created in the default directory, due to this it is required to modify the path specified in the build configuration.

Modify the created `BuildConfiguration` resource to add:

```
strategy:
  type: Docker
  dockerStrategy:
    dockerfilePath: src/main/docker/Dockerfile.jvm
```

Compile the service locally before running the OCP build. In this case, the dockerfile requires a compiled application:

```
mvn clean package
```

Start the build from the `price-service`:

```
oc start-build price-service --from-dir=. --follow
```

And deploy it:

```
oc new-app --name=price-service --image-stream=price-service:latest
```

This will provide a `Deployment` and a `Service` for the `price-service`. 

Make sure the deployment is blue and that the service is running by executing a curl:

```
curl http://localhost:8080/entity/prices
```

## Lab 2 Configure Kafka Event to Calculate a Price

The price of each of the orders placed will be calculated as an average of all prices in the price database.

In order to configure a Kafka Consumer that processes the event of an order placed it is necessary to deploy the `dotnet-consumer-service`.

Before deploying, make sure the the kafka configuration in the file `applicationSettings.json` is correct:

```
  "KafkaConfiguration": {
    "Brokers": "kafka-cluster-kafka-bootstrap:9092",
    "Topic": "order-placed",
    "ConsumerGroup": "workshop_consumer_group"
  }
```

Mind the `topic` and `consumerGroup` should be correct if you want to process the order placed events.

To deploy the Kafka Consumer service in Openshift, it is as simple as to create a build configuration as done with other services:

```
oc new-build --name dotnet-consumer-service --binary=true
```

And run the build:

```
oc start-build dotnet-consumer-service --from-dir=. --follow
```

If the build is successful, deploy the application:

```
oc new-app --name=dotnet-consumer-service --image-stream=dotnet-consumer-service:latest
```

To publish and Event on order placed, uncomment the following line in the order service:

```
//orderPlacedEmitter.send(order);
```

And rebuild the `order-service` application:

```
oc start-build order-service --from-dir=. --follow
```

At this moment, each new order added will place an event on the topic that will be processed by the consumer and a price will be stored by the price service.

A useful tool is to consume a topic from Kafka cli, if you have access to the kafka cluster it is possible to run:

```
/kafka-console-consumer.sh --topic order-placed --bootstrap-server localhost:9092
```

Which wil regiser a cli consumer on that topic.

If you want only to check the number of events published:
```
bin/kafka-run-class.sh kafka.tools.GetOffsetShell   --broker-list localhost:9092 --topic order-placed | awk -F  ":" '{sum += $3} END {print "Result: "sum}'
```

## Lab 3 KNative Price Calculation

To explore the capabilities of the Knative Serving framework, it is possible to configure the `price-service` as a Knative Serving component. This will scale back to 0 the number of pods of the price service if there is no requests sent to the service.

In order to do this, create a `Knative Service`:

```
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: price-service-kn
  labels:
    networking.knative.dev/visibility: cluster-local
spec:
  template:
    spec:
      containers:
      - image: image-registry.openshift-image-registry.svc:5000/workshop/price-service
        ports:
        - containerPort: 8080
```

To check the knative serving url that needs to be used:

```
$ oc get ksvc
NAME            URL                                               LATESTCREATED         LATESTREADY           READY   REASON
price-service   http://price-service-kn.workshop.svc.cluster.local   price-service-00002   price-service-00002   True
```

Change the `dotnet-consumer-service` to send prices to that URL instead of `price-service:8080`

Rebuild the `dotnet-consumer-service`:

```
oc start-build dotnet-consumer-service --from-dir=. --follow
```

And evalute how the price service is only scaled up when a price is sent from the Kafka consumer.