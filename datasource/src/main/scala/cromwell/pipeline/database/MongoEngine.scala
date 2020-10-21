package cromwell.pipeline.database

//import cromwell.pipeline.database.MongoEngine.log
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
import org.slf4j.LoggerFactory

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
//  log.info(s"Mongo connection $mongoClient")
  lazy val mongoDatabase: MongoDatabase =
    mongoClient.getDatabase(mongoConfig.database)
  lazy val mongoCollection: MongoCollection[Document] =
    mongoDatabase.getCollection(mongoConfig.collection)

  override def close(): Unit = mongoClient.close()
}

object MongoEngine {
////  val log = LoggerFactory.getLogger(this.getClass)
//  val log = LoggerFactory.getLogger(MongoEngine.getClass)
////  log.info("Mongo connection" MongoEngine.mongoClient)
//  log.debug(s"reading $MongoEngine")
////  log.error(s"bad request: $req")
}
