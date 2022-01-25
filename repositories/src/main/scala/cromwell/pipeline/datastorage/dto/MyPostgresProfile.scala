package cromwell.pipeline.datastorage.dto

import com.github.tminglei.slickpg.{ ExPostgresProfile, PgEnumSupport, PgPlayJsonSupport }
import slick.basic.Capability
import slick.jdbc.JdbcType

trait MyPostgresProfile extends ExPostgresProfile with PgEnumSupport with PgPlayJsonSupport {

  def pgjson: String = "jsonb"

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate
  override val api: MyPostgresProfile.this.MyAPI.type = MyAPI
  object MyAPI extends API with JsonImplicits {
    implicit val visibilityTypeMapper: JdbcType[Visibility] =
      createEnumJdbcType[Visibility]("visibility_type", Visibility.toString, Visibility.fromString, quoteName = false)
    implicit val visibilityTypeListMapper: JdbcType[List[Visibility]] =
      createEnumListJdbcType[Visibility](
        "visibility_type",
        Visibility.toString,
        Visibility.fromString,
        quoteName = false
      )
    implicit val visibilityColumnExtensionMethodsBuilder
      : api.Rep[Visibility] => EnumColumnExtensionMethods[Visibility, Visibility] =
      createEnumColumnExtensionMethodsBuilder[Visibility]
    implicit val visibilityOptionColumnExtensionMethodsBuilder
      : api.Rep[Option[Visibility]] => EnumColumnExtensionMethods[Visibility, Option[Visibility]] =
      createEnumOptionColumnExtensionMethodsBuilder[Visibility]

    implicit val statusTypeMapper: JdbcType[Status] =
      createEnumJdbcType[Status]("status_type", Status.toString, Status.fromString, quoteName = false)
    implicit val statusTypeListMapper: JdbcType[List[Status]] =
      createEnumListJdbcType[Status](
        "status_type",
        Status.toString,
        Status.fromString,
        quoteName = false
      )
    implicit val statusColumnExtensionMethodsBuilder: api.Rep[Status] => EnumColumnExtensionMethods[Status, Status] =
      createEnumColumnExtensionMethodsBuilder[Status]
    implicit val statusOptionColumnExtensionMethodsBuilder
      : api.Rep[Option[Status]] => EnumColumnExtensionMethods[Status, Option[Status]] =
      createEnumOptionColumnExtensionMethodsBuilder[Status]
  }
}
