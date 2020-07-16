package cromwell.pipeline.datastorage.dao.repository

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{ Completed, Document, MongoCollection }

import scala.concurrent.Future

class DocumentRepository(collection: MongoCollection[Document]) {

  def addOne(document: Document): Future[Completed] =
    collection.insertOne(document).toFuture()

  def updateOne(document: Document, fieldName: String, name: String): Future[UpdateResult] =
    collection.replaceOne(equal(fieldName, name), document, ReplaceOptions().upsert(true)).toFuture()

  def getByParam(field: String, name: String): Future[Seq[Document]] =
    collection.find(equal(field, name)).toFuture()
}
