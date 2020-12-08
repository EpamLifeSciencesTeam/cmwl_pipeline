# cmwl_pipeline

Dependencies:
- Java 8 
- Scala 2.12
- SBT 1.3.0


# Getting Started

## Installation
First, install Java 8 JDK, Scala 2.12, the Scala build tool, and Docker on your computer.

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

### Finding unused dependencies
To show unused dependencies list, execute the following command:
```
sbt unusedCompileDependencies
```

### Finding undeclared dependencies
This task executes while building
To show undeclared dependencies list, execute the following command:
```
sbt undeclaredCompileDependencies
```

Default configuration can be overloaded in application.properties file located in the same directory as service jar
## Local database usage
Locally we could start Postgres and Mongo db instances via
```
docker-compose -f postgres-mongo.yml up
```
## Build With
* [SBT](https://www.scala-sbt.org/) Build and dependency management
* [AkkaHTTP](https://doc.akka.io/docs/akka-http/current/index.html)  Asynchronous, streaming-first HTTP server and client
* [Slick](http://slick.lightbend.com/) Database query and access library
* [Liquibase](https://www.liquibase.org/) Migrations
* [Circee]() JSON processing
* [ScalaTest](http://www.scalatest.org/) Unit-testing framework
* [ScalaMock](https://scalamock.org/) Unit-testing framework

## Run cromwell on aws instance
In order to run the cromwell project on aws instance. It is necessary to be connected to the EPAM vpn and select the Russia gateway (vpn-ru.epam.com).

### Run the workflows through swagger

Once that you're connected, follow this [link](http://ec2-54-218-82-67.us-west-2.compute.amazonaws.com/swagger/index.html?url=/swagger/cromwell.yaml) to try/execute the services through the swagger interface.

Run the next workflows:
- _POST /api/workflows/{version}_ and fill the next two options 
    - For the `workflowSource file` option select the `cromwell-saample/hello.wdl` file that was created before.
    - For the `workflowInouts file` option select the `cromwell-saample/input.json` file.
Submit the request, and a response like the below will be returned.
```
{
  "id": "56e252bc-c83a-46e0-b311-9bc26f0038f8", 
  "status": "Submitted" 
}
```

- _GET /api/workflows/{version}/{id}/status_ copy the id returned by the previous response
    - For the id paste the id that was copied.
Submit the request, and a response like the below will be returned.
```
{
  "status": "Succeeded", 
  "id": "56e252bc-c83a-46e0-b311-9bc26f0038f8" 
} 
```  

### Run the workflows through curl, postman or another client

Run the next commands
- `curl -X POST "http://ec2-54-218-82-67.us-west-2.compute.amazonaws.com/api/workflows/v1" -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "workflowSource=@hello.wdl" -F workflowInputs=@inputs.json;type=application/json `
    - A response with the workflow id will be returned.
- `curl -X GET "http://ec2-54-218-82-67.us-west-2.compute.amazonaws.com/api/workflows/v1/{workflow_id}}/status" -H "accept: application/json"`
    - Replace the `workflow_id` with the id returned by the previous response.
    - A response with the workflow status will be returned.
 
## Developer Guide
https://kb.epam.com/display/EPMLSTR/Cromwell+Developer+Guide
