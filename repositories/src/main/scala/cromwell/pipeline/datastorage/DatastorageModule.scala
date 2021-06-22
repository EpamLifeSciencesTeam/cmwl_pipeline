package cromwell.pipeline.datastorage

import cromwell.pipeline.database.{ MongoEngine, PipelineDatabaseEngine }
import cromwell.pipeline.datastorage.dao.entry.{ AliasesSupport, ProjectEntry, RunEntry, UserEntry }
import cromwell.pipeline.datastorage.dao.mongo.DocumentRepository
import cromwell.pipeline.datastorage.dao.repository.{
  ProjectConfigurationRepository,
  ProjectRepository,
  RunRepository,
  UserRepository
}
import cromwell.pipeline.datastorage.dto.{ CustomsWithEnumSupport, PipelineVersion }
import cromwell.pipeline.model.validator.{ Enable, Wrapped }
import cromwell.pipeline.model.wrapper.{ Name, RunId, UserEmail, UserId }
import cromwell.pipeline.utils.ApplicationConfig
import org.mongodb.scala.{ Document, MongoCollection }
import slick.jdbc.JdbcProfile
import slick.lifted.{ Isomorphism, Rep, StringColumnExtensionMethods }

import scala.concurrent.ExecutionContext

class DatastorageModule(applicationConfig: ApplicationConfig)(implicit executionContext: ExecutionContext) {

  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = new PipelineDatabaseEngine(applicationConfig.config)
  lazy val profile: JdbcProfile = pipelineDatabaseEngine.profile
  lazy val databaseLayer: DatabaseLayer = new DatabaseLayer(profile)
  lazy val mongoCollection: MongoCollection[Document] =
    new MongoEngine(applicationConfig.mongoConfig).mongoCollection
  lazy val documentRepository: DocumentRepository = new DocumentRepository(mongoCollection)

  lazy val userRepository: UserRepository =
    UserRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val projectRepository: ProjectRepository =
    ProjectRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val runRepository: RunRepository =
    RunRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val configurationRepository: ProjectConfigurationRepository =
    ProjectConfigurationRepository(documentRepository)
}

trait Profile {
  val profile: JdbcProfile
  object Implicits {

    import scala.language.implicitConversions

    implicit def uuidIso: Isomorphism[UserId, String] = iso[UserId, String](_.unwrap, UserId(_, Enable.Unsafe))
    implicit def runidIso: Isomorphism[RunId, String] = iso[RunId, String](_.unwrap, RunId(_, Enable.Unsafe))
    implicit def emailIso: Isomorphism[UserEmail, String] =
      iso[UserEmail, String](_.unwrap, UserEmail(_, Enable.Unsafe))
    implicit def nameIso: Isomorphism[Name, String] = iso[Name, String](_.unwrap, Name(_, Enable.Unsafe))

    implicit def pipelineVersionIso: Isomorphism[PipelineVersion, String] =
      iso[PipelineVersion, String](_.name, PipelineVersion(_))

    implicit def wrappedStringColumnExtension[T <: Wrapped[String]](c: Rep[T]): StringColumnExtensionMethods[String] =
      new StringColumnExtensionMethods[String](c.asInstanceOf[Rep[String]])

    private def iso[A, B](map: A => B, comap: B => A) = new Isomorphism(map, comap)
  }
}

class DatabaseLayer(override val profile: JdbcProfile)
    extends Profile
    with AliasesSupport
    with UserEntry
    with ProjectEntry
    with CustomsWithEnumSupport
    with RunEntry
