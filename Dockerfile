FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1 as builder

WORKDIR /cromwell_pipeline
ADD . /cromwell_pipeline

RUN sbt clean portal/assembly

FROM openjdk:8-jre-alpine
WORKDIR /cromwell_pipeline
COPY --from=builder /cromwell_pipeline/portal/target/scala-2.12/portal.jar /cromwell_pipeline/