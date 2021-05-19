# cmwl_pipeline

Dependencies:
- Java 11
- Scala 2.12.9
- SBT 1.3.4


# Getting Started

## Installation
### required dependencies installation
Install Java 11 JDK, Scala 2.12.9, the Scala build tool

### docker installation
To install Docker and docker-compose CLI, run the **scripts/install-docker.sh** script.

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

## Run Cromwell Pipeline in the Docker (Local)

- Create .env file with environment variables from .env.example file in the root folder
- Run `sbt clean docker:publishLocal` for creating Pipeline docker image using the local Docker server
- Run `docker-compose up` for running all docker containers or `docker run -d --name cmwl_pipeline -p 8080:8080 cmwl_pipeline:0.1` for running  only Pipeline
- Use `http://localhost:8080` address for sending request
 
## File upload process in Cromwell Pipeline
### Step 1: WDL file validation
For validation WDL file send POST request <br>
to address `<cromwell_pipeline_url>/files/validation` <br>
with header `Authorization <auth_token>` <br>
and request body:
```
{
    "content": "<file_content>"
}
```
* Instead of `<cromwell_pipeline_url>` insert the application URL
* Instead of `<auth_token>` insert the authorization token
* Instead of `<file_content>` insert the content from [cromwell-sample/hello.wdl](https://github.com/EpamLifeSciencesTeam/cmwl_pipeline/blob/a2a82a63ed8c66dc881892c8645e40ee58a36c1a/cromwell-sample/hello.wdl) file

If validation succeed, response OK with HTTP status 200 will be returned.
### Step 2: Create project (if doesn't exist)
To create project send POST request <br>
to address `<cromwell_pipeline_url>/projects` <br>
with header `Authorization <auth_token>` <br>
and request body:
```
{
    "name": "<project_name>"
}
```
* Instead of `<cromwell_pipeline_url>` insert the application URL
* Instead of `<auth_token>` insert the authorization token
* Instead of `<project_name>` insert the name of new project

If creation succeed, response OK with HTTP status 200 will be returned.

### Get project by name (if exists)
To get project by name send GET request <br>
to address `<cromwell_pipeline_url>/projects?name=<project_name>` <br>
with header `Authorization <auth_token>` <br>
* Instead of `<cromwell_pipeline_url>` insert the application URL
* Instead of `<auth_token>` insert the authorization token

If project exists, response OK with HTTP status 200, and the following response body will be returned:
```
{
    "projectId": "<project_id>",
    "ownerId": "<owner_id>",
    "name": "<project_name>",
    "active": true,
    "repositoryId": <repository_id>,
    "visibility": "<visibility>"
}
```
### Step 3: Upload file to project
To upload file into existing project send POST request <br>
to address `<cromwell_pipeline_url>/files` <br>
with header `Authorization <auth_token>` <br>
and request body:
```
{
    "projectId": "<project_id>",
    "projectFile": {
        "path": "<file_name>",
        "content": "<file_content>"
    }
}
```
* Instead of `<cromwell_pipeline_url>` insert the application URL
* Instead of `<auth_token>` insert the authorization token
* Instead of `<project_id>` insert the project id from response in Step 2 (Get project by name)
* Instead of `<file_name>` insert the file name ("hello.wdl")
* Instead of `<file_content>` insert the content from [cromwell-sample/hello.wdl](https://github.com/EpamLifeSciencesTeam/cmwl_pipeline/blob/a2a82a63ed8c66dc881892c8645e40ee58a36c1a/cromwell-sample/hello.wdl) file

If uploading succeed, response OK with HTTP status 200, and the following response body will be returned:
```
{
    "file_path": "<file_name>",
    "branch": "master"
}
```
### Step 4: Build project configuration by project
To build project configuration by project send POST request <br>
to address `<cromwell_pipeline_url>/files/configurations` <br>
with header `Authorization <auth_token>` <br>
and request body:
```
{
    "projectId": "<project_id>",
    "projectFilePath": "<file_name>",
}
```
* Instead of `<cromwell_pipeline_url>` insert the application URL
* Instead of `<auth_token>` insert the authorization token
* Instead of `<project_id>` insert the id of project, which configuration you want to build
* Instead of `<file_name>` insert the file name ("hello.wdl")

If building succeed, response OK with HTTP status 200, and the following response body will be returned:
```
{
    "projectId": "<project_id>",
    "active": true,
    "projectFileConfigurations": [
        {
            "path": "<file_name>",
            "inputs": [
                {
                    "name": "forkjoin.grep.float",
                    "typedValue": {
                        "_type": "Float"
                    }
                },
                {
                    "name": "forkjoin.grep.pattern",
                    "typedValue": {
                        "_type": "String"
                    }
                }
            ]
        }
    ]
}
```
### Step 5: Add configuration to project
To add configuration to the project send PUT request <br>
to address `<cromwell_pipeline_url>/configurations` <br>
with header `Authorization <auth_token>` <br>
and request body (JSON from response in Step 4):
```
{
    "projectId": "<project_id>",
    "active": true,
    "projectFileConfigurations": [
        {
            "path": "<file_name>",
            "inputs": [
                {
                    "name": "forkjoin.grep.float",
                    "typedValue": {
                        "_type": "Float"
                    }
                },
                {
                    "name": "forkjoin.grep.pattern",
                    "typedValue": {
                        "_type": "String"
                    }
                }
            ]
        }
    ]
}
```
If adding succeed, response OK with HTTP status 200 will be returned.
## Developer Guide
https://kb.epam.com/display/EPMLSTR/Cromwell+Developer+Guide
