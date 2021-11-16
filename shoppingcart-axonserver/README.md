Axon Sample Application using Axon Server
=========================================

This Axon Framework demo application focuses around a simple shopping domain, designed to show various aspects of the framework. The app can be run in various modes, using [Spring-boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-profiles.html): by selecting a specific profile, only the corresponding parts of the app will be active. Select none, and the default behaviour is activated, which activates everything. This way you can experiment with Axon in a (structured) monolith as well as in micro-services.

Where to find more information:
-------------------------------

* The [Axon Reference Guide](https://docs.axoniq.io/reference-guide/) is definitive guide on the Axon Framework and Axon Server.
* Visit [www.axoniq.io](https://www.axoniq.io) to find out about AxonIQ, the team behind the Axon Framework and Server.
* Subscribe to the [AxonIQ Youtube channel](https://www.youtube.com/AxonIQ) to get the latest Webinars, announcements, and customer stories.
* The latest version of the Shopping Cart App can be found [on GitHub](https://github.com/AxonIQ/shoppingcartshoppingcart-axonserver-demo).
* Docker images for Axon Server are pushed to [Docker Hub](https://hub.docker.com/u/axoniq).

The Shopping Cart app
---------------------

### Structure of the App
The ShoppingCart application is split into four parts, using four sub-packages of `io.axoniq.demo.shoppingcartshoppingcart`:

* The `api` package contains the ([Kotlin](https://kotlinlang.org/)) sourcecode of the messages and entity. They form the API (sic) of the application.
* The `command` package contains the Shopping Cart Aggregate class, with all command- and associated eventsourcing handlers.
* The `query` package provides the query handlers, with their associated event handlers.
* The `gui` package contains the [Vaadin](https://vaadin.com/)-based Web GUI.

Of these packages, `command`, `query`, and `gui` are also configured as profiles.

### Building the ShoppingCart app from the sources

To build the demo app, simply run the provided [Maven wrapper](https://www.baeldung.com/maven-wrapper):

```
mvnw clean package
```
Note that for Mac OSX or Linux you probably have to add "`./`" in front of `mvnw`.

Running the Shopping Cart app
-----------------------------

The simplest way to run the app is by using the Spring-boot maven plugin:

```
mvnw spring-boot:run
```
However, if you have copied the jar file `shoppingcart-axonserver-1.0.jar` from the Maven `target` directory to some other location, you can also start it with:

```
java -jar shoppingcart-axonserver-1.0.jar
```
The Web GUI can be found at [`http://localhost:8080`](http://localhost:8080).

If you want to activate only the `command` profile, use:

```
java -Dspring.profiles.active=command shoppingcart-axonserver-1.0.jar
```

Idem for `query` and `gui`.

### Running the Shopping Cart app as micro-services

To run the Shopping Cart app as if it were three seperate micro-services, use the Spring-boot `spring.profiles.active` option as follows:

```
$ java -Dspring.profiles.active=command -jar ./target/shoppingcart-axonserver-1.0.jar
$ mvn spring-boot:run -Dspring-boot.run.profiles=gui
```

This will start only the command part. To complete the app, open two other command shells, and start one with profile `query`, and the last one with `gui`. Again you can open the Web GUI at [`http://localhost:8080`](http://localhost:8080). The three parts of the application work together through the running instance of the Axon Server, which distributes the Commands, Queries, and Events.

Running Axon Server
-------------------

By default the Axon Framework is configured to expect a running Axon Server instance, and it will complain if the server is not found. To run Axon Server, you'll need a Java runtime (JRE versions 8 through 10 are currently supported, Java 11 still has Spring-boot related growing-pains).  A copy of the server JAR file has been provided in the demo package. You can run it locally, in a Docker container (including Kubernetes or even Mini-kube), or on a separate server.

### Running Axon Server locally

To run Axon Server locally, all you need to do is put the server JAR file in the directory where you want it to live, and start it using:

```
java -jar axonserver-4.1-6.jar
```

You will see that it creates a subdirectory `data` where it will store its information.

### Running Axon Server in a Docker container

To run Axon Server in Docker you can use the image provided on Docker Hub:

```
$ docker run -d --name my-axon-server -p 8024:8024 -p 8124:8124 axoniq/axonserver
...some container id...
$
```

*WARNING* This is not a supported image for production purposes. Please use with caution.

If you want to run the clients in Docker containers as well, and are not using something like Kubernetes, use the "`--hostname`" option of the `docker` command to set a useful name like "axonserver", and pass the `AXONSERVER_HOSTNAME` environment variable to adjust the properties accordingly:

```
$ docker run -d --name my-axon-server -p 8024:8024 -p 8124:8124 --hostname axonserver -e AXONSERVER_HOSTNAME=axonserver axoniq/axonserver
```

When you start the client containers, you can now use "`--link axonserver`" to provide them with the correct DNS entry. The Axon Server-connector looks at the "`axon.axonserver.servers`" property to determine where Axon Server lives, so don't forget to set it to "`axonserver`".


