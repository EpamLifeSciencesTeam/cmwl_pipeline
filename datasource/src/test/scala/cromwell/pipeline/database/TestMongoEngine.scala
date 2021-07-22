package cromwell.pipeline.database

import cromwell.pipeline.utils.MongoConfig
import org.mongodb.scala.{ MongoClientSettings, ServerAddress }

import scala.collection.JavaConverters._

class TestMongoEngine(mongoConfig: MongoConfig) extends MongoEngine(mongoConfig) {
  override lazy val mongoSettings: MongoClientSettings = MongoClientSettings
    .builder()
    .applyToSslSettings(block => block.enabled(false))
    .applyToClusterSettings(
      block => block.hosts(List(ServerAddress("localhost", mongoConfig.port)).asJava)
    )
    .build()
}
