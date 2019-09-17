# cmwl_pipeline

Dependencies:
- Java 8 
- Scala 2.12
- SBT 1.3.0


# Getting Started

## Installation
First, install Java 8 JDK, Scala 2.12, the Scala build tool, and Docker on your computer. For more instructions, see [Getting Started](https://kb.epam.com/display/EPMLSTR/Cromwell+Getting+Started).

To install Docker and docker-compose CLI, run the **scripts/install-docker.sh** script.

Install `git-hooks`, execute the following command:
```bash
bash hooks/install.sh
```

## Compiling
To compile from the command line, use the command below:
```
    sbt compile
```

##  Running Locally
To run the application, use the following command:
```
    sbt run
```

## Running the Tests
The tree below reflects how unit tests and integration tests are separated by different directories.

```
  -src
    |_it   // Here, you add new tests which load application context
    |      // and connect to datasources deployed on staging.
    |
    |_test // Here, you add pure unit tests which cover one or several specific
           // `cromwell.pipeline.components` and have no dependencies on infrastructure.

```

### Running Unit Tests Locally
Use the command below to run unit tests:
```
    sbt test
```

### Running Integration Tests Locally
To run the integration test suit, execute the following command:
```
sbt it:test
```
Default configuration can be overloaded in application.properties file located in the same directory as service jar
## Local database usage
Locally we could start Postgres db instance via
```
docker-compose up
```
## Build With
* [SBT](https://www.scala-sbt.org/) Build and dependency management
* [AkkaHTTP](https://doc.akka.io/docs/akka-http/current/index.html)  Asynchronous, streaming-first HTTP server and client
* [Slick](http://slick.lightbend.com/) Database query and access library
* [Liquibase](https://www.liquibase.org/) Migrations
* [Circee]() JSON processing
* [ScalaTest](http://www.scalatest.org/) Unit-testing framework
* [ScalaMock](https://scalamock.org/) Unit-testing framework

## Developer Guide
https://kb.epam.com/display/EPMLSTR/Cromwell+Developer+Guide
