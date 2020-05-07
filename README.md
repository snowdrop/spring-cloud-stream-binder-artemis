# Table of contents

- [Notice](#notice)
- [Overview](#overview)
- [Configuration](#configuration)
- [Usage examples](#usage-examples)
- [Notes on implementation details](#notes-on-implementation-details)
- [Building and testing the project](#building-and-testing-the-project)
- [Contributing](#contributing)
- [Releasing](#releasing)

# Notice

This project is under development and not all expected features have yet been implemented.
So, please, use with caution.

# Overview

[![Build Status](https://circleci.com/gh/snowdrop/spring-cloud-stream-binder-artemis.svg?style=shield)](https://circleci.com/gh/snowdrop/spring-cloud-stream-binder-artemis/tree/master)

This binder enables Spring Cloud Stream applications to use Apache Artemis message broker as a connection medium.
It is transparent to the application code and can be enabled with a Maven dependency and Spring Boot configuration in the same way as any other Spring Cloud Stream binder.

It is compatible with Apache Artemis 2.4.0, Spring Boot 2.0.7.RELEASE and Spring Cloud Stream Elmhurst.SR2.
For more information on Spring Cloud Stream please refer to its [documentation](https://docs.spring.io/spring-cloud-stream/docs/Elmhurst.SR2/reference/htmlsingle).

# Configuration

## Artemis broker configuration

This binder depends on Spring Boot Artemis integration to locate Artemis broker.
So please refer to its [documentation](https://docs.spring.io/spring-boot/docs/2.0.7.RELEASE/reference/html/boot-features-messaging.html#boot-features-artemis) in order to setup that.

## Spring Cloud Stream configuration

Artemis binder supports Spring Cloud Stream configuration as described in its [documentation](https://docs.spring.io/spring-cloud-stream/docs/Elmhurst.SR2/reference/htmlsingle/#_configuration_options).

## Artemis address configuration

For each configured destination Artemis address is created using default broker address settings.
Following properties can be used to override them for addresses created by this binder assuming the user has management permissions.

| Property | Description | Default value |
| -------- | ----------- | ------------- |
| modifyAddressSettings | Whether binder should modify address settings for addresses it's creating | false |
| managementAddress | Broker management address  | activemq.management |
| autoBindDeadLetterAddress | Whether binder should create a dead letter address for each destination | false |
| autoBindExpiryAddress | Whether binder should create an expiry address for each destination  | false |
| brokerExpiryDelay | Message expiration time override (-1 don't override) | -1 |
| brokerRedeliveryDelay | Time to redeliver a message (in ms) | 0 |
| brokerMaxRedeliveryDelay | Max value for the redeliveryDelay | brokerRedeliveryDelay * 10 |
| brokerRedeliveryDelayMultiplier | Multiplier to apply to the redeliveryDelay | 1.0 |
| brokerMaxDeliveryAttempts | Number of retries before dead letter address | 10 |
| brokerSendToDlaOnNoRoute | Forward messages to a dead letter address when no queues subscribing | false |

# Usage examples

Artemis binder does not impose any other usage requirements from a generic Spring Cloud Stream application. Please refer to a Spring Cloud Stream [documentation](https://docs.spring.io/spring-cloud-stream/docs/Elmhurst.SR2/reference/htmlsingle) for a detailed usage explanation.

## Example applications
| Name | Description |
| ---- | ----------- |
| [Multi IO](./spring-cloud-starter-stream-artemis-samples/multi-io) | This example is a multi-io sample application from Spring Cloud Stream samples repository configured to use Artemis binder and an embedded Apache Artemis broker.

# Notes on implementation details

## Address and queue conventions

When creating addresses and queues binder uses the following conventions (make sure there are no conflicts with other broker users):
* Destination addresses and queues use multicast
* Unpartitioned destination address is named the same as the destination
* Partitioned destination addresses are named the same as the destination and suffixed with partition number e.g. test-0, test-1
* Queues for consumers are named as a combination of an address and a group name (or a generated string for anonymous consumers) e.g. test-output, test-0-input
* Destination specific dead letter addresses and queues are named the same as the destination address with a suffix ".dlq"
* Destination specific expiry addresses and queues are named the same as the destination address with a suffix ".exp"

## Retry template error conventions

Consumer retry template is enabled if `maxAttempts` property is set to a number higher than 1.
In that case failed deliveries will be sent to an error channel.
Error channel is named using the following patter: `{destination name}[-{partition number}]-{group name or generated string}.errors`, e.g. test-output.errors, test-0-input.errors


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

## With GitHub action

GitHub release action is activates with a tag.
For example, to release a version 1.2.3 create and push the following tag.
```bash
git tag release-1.2.3
git push origin release-1.2.3
```

## Manually

Dry run:
```
./mvnw release:prepare -DdryRun
```

Tag:
```
./mvnw release:prepare
```

Deploy:
```
./mvnw release:perform
```
