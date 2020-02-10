package cromwell.pipeline.datastorage

import com.softwaremill.macwire._
import com.typesafe.config.Config
import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.{ ProjectEntry, UserEntry }
import cromwell.pipeline.datastorage.dao.repository.{ ProjectRepository, UserRepository }
import slick.jdbc.JdbcProfile
import cromwell.pipeline.datastorage.dto.{ Name, UUID, UserEmail }
import cromwell.pipeline.utils.validator.Wrapped
import slick.lifted.StringColumnExtensionMethods

@Module
class DatastorageModule(config: Config) {
  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = wireWith(PipelineDatabaseEngine.fromConfig _)
  lazy val profile: JdbcProfile = pipelineDatabaseEngine.profile
  lazy val databaseLayer: DatabaseLayer = wire[DatabaseLayer]

  lazy val userRepository: UserRepository = wire[UserRepository] //wires databaseLayer
  lazy val projectRepository: ProjectRepository = wire[ProjectRepository] //wires databaseLayer
}

trait Profile {
  val profile: JdbcProfile
  object Implicits {

    import profile.api._
    import cats.implicits.catsStdShowForString
    import scala.language.implicitConversions

    implicit def uuidIso: Isomorphism[UUID, String] = iso[UUID, String](_.unwrap, UUID(_))
    implicit def emailIso: Isomorphism[UserEmail, String] = iso[UserEmail, String](_.unwrap, UserEmail(_))
    implicit def nameIso: Isomorphism[Name, String] = iso[Name, String](_.unwrap, Name(_))

    implicit def wrappedStringColumnExtension[T <: Wrapped[String]](c: Rep[T]): StringColumnExtensionMethods[String] =
      new StringColumnExtensionMethods[String](c.asInstanceOf[Rep[String]])

    private def iso[A, B](map: A => B, comap: B => A) = new Isomorphism(map, comap)
  }
}

// https://books.underscore.io/essential-slick/essential-slick-3.html#scaling-to-larger-codebases
class DatabaseLayer(val profile: JdbcProfile) extends Profile with UserEntry with ProjectEntry
