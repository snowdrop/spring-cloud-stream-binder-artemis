# Table of contents

- [Notice](#notice)
- [Overview](#overview)
- [Configuration](#configuration)
- [Usage examples](#usage-examples)
- [Building and testing the project](#building-and-testing-the-project)
- [Contributing](#contributing)
- [Releasing](#releasing)

# Notice

This project is under development and not all expected features have yet been implemented. So, please, use with caution.

# Overview

[![Build Status](https://travis-ci.org/snowdrop/spring-cloud-stream-binder-artemis.svg?branch=master)](https://travis-ci.org/snowdrop/spring-cloud-stream-binder-artemis)

This binder enables Spring Cloud Stream applications to use Apache Artemis message broker as a connection medium. It is transparent to the application code and can be enabled with a Maven dependency and Spring Boot configuration in the same way as any other Spring Cloud Stream binder.

It is compatible with Apache Artemis 2.1.0, Spring Boot 1.5.6.RELEASE and Spring Cloud Stream Chelsea.SR2. For more information on Spring Cloud Stream please refer to its [documentation](https://docs.spring.io/spring-cloud-stream/docs/Chelsea.SR2/reference/htmlsingle).

# Configuration

## Artemis broker configuration

This binder depends on a Spring Boot Artemis integration to locate Artemis broker. So please refer to its [documentation](https://docs.spring.io/spring-boot/docs/1.5.6.RELEASE/reference/html/boot-features-messaging.html#boot-features-artemis) in order to setup that.

## Artemis binder configuration

Following are the Artemis binder specific properties (the list will increase as implementation matures):

| Name | Required | Default value | Description |
| ---- | -------- | ------------- | ----------- |
| spring.cloud.stream.artemis.binder.transport | yes | - | Transport to be used to connect to an Artemis server e.g. `org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory` or `org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory` |

## Spring Cloud Stream configuration

Artemis binder supports Spring Cloud Stream configuration as described in its [documentation](https://docs.spring.io/spring-cloud-stream/docs/Chelsea.SR2/reference/htmlsingle/#_configuration_options).

Note: support for consumer retry has not yet been implemented. Thus, following properties are no supported: `maxAttempts`, `backOffInitialInterval`, `backOffMaxInterval`, `backOffMultiplier`.

# Usage examples

Artemis binder does not impose any other usage requirements from a generic Spring Cloud Stream application. Please refer to a Spring Cloud Stream [documentation](https://docs.spring.io/spring-cloud-stream/docs/Chelsea.SR2/reference/htmlsingle) for a detailed usage explanation.

## Example applications
| Name | Description |
| ---- | ----------- |
| [External broker example](https://github.com/gytis/spring-cloud-stream-artemis-sample) | This example is a [multi-io](https://github.com/spring-cloud/spring-cloud-stream-samples/blob/master/multi-io) application from Spring Cloud Stream samples repository configured to use Artemis binder and remote Apache Artemis broker.

# Building and testing the project

To build a project simply use a Maven Wrapper provided in this repository:

```
./mvnw clean install -DskipTests
```

Binder tests are split into two parts:

1. Unit tests, which are located in the same module as the binder implementation: [spring-cloud-stream-binder-artemis](./spring-cloud-stream-binder-artemis)
2. Integration tests, which are located in their own module: [spring-cloud-starter-stream-artemis-it](./spring-cloud-starter-stream-artemis-it)

Tests are executed by default when building a project without any parameters:
```
./mvnw clean install
```

# Contributing

We're always happy to get help from the community. So do not hesitate to raise a pull request with a new feature, a bug fix or any other improvement.

Also, if you stumble upon an issues or think that some feature should be added, please, create an issue on [GitHub](https://github.com/snowdrop/spring-cloud-stream-binder-artemis/issues) or [JIRA](https://issues.jboss.org/projects/SB).

# Releasing

Dry run:
```
mvn release:prepare -DdryRun
```

Tag:
```
mvn release:prepare
```

Deploy:
```
mvn release:perform -DskipTests
```