package cromwell.pipeline.datastorage

import cromwell.pipeline.database.{ MongoEngine, PipelineDatabaseEngine }
import cromwell.pipeline.datastorage.dao.ProjectEntry
import cromwell.pipeline.datastorage.dao.entry.{ RunEntry, UserEntry }
import cromwell.pipeline.datastorage.dao.repository.{
  DocumentRepository,
  ProjectRepository,
  RunRepository,
  UserRepository
}
import cromwell.pipeline.datastorage.dto.CustomsWithEnumSupport
import cromwell.pipeline.model.validator.{ Enable, Wrapped }
import cromwell.pipeline.model.wrapper.{ Name, RunId, UserEmail, UserId }
import cromwell.pipeline.utils.ApplicationConfig
import org.mongodb.scala.{ Document, MongoCollection }
import slick.jdbc.JdbcProfile
import slick.lifted.StringColumnExtensionMethods

class DatastorageModule(applicationConfig: ApplicationConfig) {

  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = new PipelineDatabaseEngine(applicationConfig.config)
  lazy val profile: JdbcProfile = pipelineDatabaseEngine.profile
  lazy val databaseLayer: DatabaseLayer = new DatabaseLayer(profile)
  lazy val configurationCollection
    : MongoCollection[Document] = new MongoEngine(applicationConfig.mongoConfig).mongoCollection

  lazy val userRepository: UserRepository =
    new UserRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val projectRepository: ProjectRepository =
    new ProjectRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val runRepository: RunRepository =
    new RunRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val configurationRepository: DocumentRepository = new DocumentRepository(configurationCollection)
}

trait Profile {
  val profile: JdbcProfile
  object Implicits {

    import cats.implicits.catsStdShowForString
    import profile.api._

    import scala.language.implicitConversions

    implicit def uuidIso: Isomorphism[UserId, String] = iso[UserId, String](_.unwrap, UserId(_, Enable.Unsafe))
    implicit def runidIso: Isomorphism[RunId, String] = iso[RunId, String](_.unwrap, RunId(_, Enable.Unsafe))
    implicit def emailIso: Isomorphism[UserEmail, String] =
      iso[UserEmail, String](_.unwrap, UserEmail(_, Enable.Unsafe))
    implicit def nameIso: Isomorphism[Name, String] = iso[Name, String](_.unwrap, Name(_, Enable.Unsafe))

    implicit def wrappedStringColumnExtension[T <: Wrapped[String]](c: Rep[T]): StringColumnExtensionMethods[String] =
      new StringColumnExtensionMethods[String](c.asInstanceOf[Rep[String]])

    private def iso[A, B](map: A => B, comap: B => A) = new Isomorphism(map, comap)
  }
}

class DatabaseLayer(override val profile: JdbcProfile)
    extends Profile
    with UserEntry
    with ProjectEntry
    with CustomsWithEnumSupport
    with RunEntry
