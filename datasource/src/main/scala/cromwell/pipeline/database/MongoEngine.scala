package cromwell.pipeline.database

import cromwell.pipeline.utils.MongoConfig
import org.mongodb.scala.{
  Document,
  MongoClient,
  MongoClientSettings,
  MongoCollection,
  MongoCredential,
  MongoDatabase,
  ServerAddress
}

import scala.collection.JavaConverters._

class MongoEngine(mongoConfig: MongoConfig) extends AutoCloseable {
  lazy val mongoCredential: MongoCredential = MongoCredential.createCredential(
    mongoConfig.user,
    mongoConfig.authenticationDatabase,
    mongoConfig.password
  )
  lazy val mongoSettings: MongoClientSettings = MongoClientSettings
    .builder()
    .applyToSslSettings(p => p.enabled(false))
    .applyToClusterSettings(
      p => p.hosts(List(ServerAddress(mongoConfig.host, mongoConfig.port)).asJava)
    )
    .credential(mongoCredential)
    .build()
  lazy val mongoClient: MongoClient = MongoClient(mongoSettings)
  lazy val mongoDatabase: MongoDatabase =
    mongoClient.getDatabase(mongoConfig.database)
  lazy val mongoCollection: MongoCollection[Document] =
    mongoDatabase.getCollection(mongoConfig.collection)

  override def close(): Unit = mongoClient.close()
}
