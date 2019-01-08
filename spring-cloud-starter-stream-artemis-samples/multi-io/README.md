Spring Cloud Stream Multi IO Sample
====================================

In this *Spring Cloud Stream* sample, the application shows how to configure multiple input/output channels inside a single application.

## Notice

This sample is a copy of [multi-io](https://github.com/spring-cloud/spring-cloud-stream-samples/blob/master/multi-io-samples/multi-io) sample adapted for Artemis Spring Cloud Stream binder.

## Requirements

To run this sample, you will need to have installed:

* Java 8 or Above

## Code Tour

This sample is a Spring Boot application that bundles multiple application together to showcase how to configure multiple input/output channels.

* MultipleIOChannelsApplication - the Spring Boot Main Application
* SampleSource - the app that configures two output channels (output1 and output2).
* SampleSink - the app that configures two input channels (input1 and input2).

The channels output1 and input1 connect to the same destination (test1) on the broker (Artemis) and the channels output2 and
input2 connect to the same destination (test2) on Artemis.
For demo purpose, the apps `SampleSource` and `SampleSink` are bundled together.
In practice they are separate applications unless bundled together by the `AggregateApplicationBuilder`.

## Running the application

* Go to the application root

* `./mvnw clean package`

* `java -jar target/multi-io.jar`

The application will log messages from source and sink.
Source sends a message every second which will be consumed by the sink.
You will see output similar to the following every second.

```
******************
From Source1
******************
Sending value: FromSource1
******************
From Source2
******************
Sending value: FromSource2
******************
At Sink2
******************
Received message FromSource2
******************
At Sink1
******************
Received message FromSource1
```


