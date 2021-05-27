package cromwell.pipeline.utils.configs

import cromwell.pipeline.utils._
import cromwell.pipeline.utils.configs.SecretData._
import play.api.libs.functional.syntax.{ toFunctionalBuilderOps, unlift }
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{ Json, Writes, __ }

trait ConfigJsonOps {
  def configToJsonString(applicationConfig: ApplicationConfig): String
}

object ConfigJsonOps extends ConfigJsonOps {

  def configToJsonString(applicationConfig: ApplicationConfig): String = {

    val secretCAWrites: Writes[SecretData[Array[Char]]] = secretDataWrites(Writes.StringWrites.contramap(new String(_)))

    implicit val wsWrites: Writes[WebServiceConfig] =
      ((__ \ "interface").write[String] ~
        (__ \ "port").write[Int])(unlift(WebServiceConfig.unapply))

    implicit val expTimeWrites: Writes[ExpirationTimeInSeconds] =
      ((__ \ "accessToken").write[Long] ~
        (__ \ "refreshToken").write[Long] ~
        (__ \ "userSession").write[Long])(unlift(ExpirationTimeInSeconds.unapply))

    implicit val authWrites: Writes[AuthConfig] =
      ((__ \ "secretKey").write[SecretData[String]] ~
        (__ \ "hmacAlgorithm").write[String] ~
        (__ \ "expirationTimeInSeconds").write[ExpirationTimeInSeconds])(
        authConfig =>
          unlift(AuthConfig.unapply)(authConfig).copy(_1 = authConfig.secretKey, _2 = authConfig.hmacAlgorithm.toString)
      )

    implicit val gitLabWrites: Writes[GitLabConfig] =
      ((__ \ "url").write[String] ~
        (__ \ "token").write[SecretData[Map[String, String]]] ~
        (__ \ "defaultFileVersion").write[String] ~
        (__ \ "defaultBranch").write[String])(
        gitLabConfig => unlift(GitLabConfig.unapply)(gitLabConfig).copy(_2 = gitLabConfig.token)
      )

    implicit val mongoWrites: Writes[MongoConfig] =
      ((__ \ "user").write[String] ~
        (__ \ "password").write[SecretData[Array[Char]]](secretCAWrites) ~
        (__ \ "host").write[String] ~
        (__ \ "port").write[Int] ~
        (__ \ "authenticationDatabase").write[String] ~
        (__ \ "database").write[String] ~
        (__ \ "collection").write[String])(
        mongoConfig => unlift(MongoConfig.unapply)(mongoConfig).copy(_2 = mongoConfig.password)
      )

    implicit val postgreWrites: Writes[PostgreConfig] =
      ((__ \ "serverName").write[String] ~
        (__ \ "portNumber").write[Int] ~
        (__ \ "databaseName").write[String] ~
        (__ \ "user").write[String] ~
        (__ \ "password").write[SecretData[Array[Char]]](secretCAWrites))(
        postgreConfig => unlift(PostgreConfig.unapply)(postgreConfig).copy(_5 = postgreConfig.password)
      )

    implicit val appConfigWrites: Writes[ApplicationConfig] =
      ((__ \ "WebServiceConfig").write[WebServiceConfig] ~
        (__ \ "AuthConfig").write[AuthConfig] ~
        (__ \ "GitLabConfig").write[GitLabConfig] ~
        (__ \ "MongoConfig").write[MongoConfig] ~
        (__ \ "PostgreConfig").write[PostgreConfig])(unlift(ApplicationConfig.unapply))

    val messageHeader: String = {
      val headerDelimiter = "-"
      val headerMessage = "Application starts with the following configuration:"
      val headerDelimiterLine = s"${headerDelimiter * headerMessage.length}"

      s"\n$headerDelimiterLine\n$headerMessage\n$headerDelimiterLine\n"
    }

    val configMessage = Json.prettyPrint(Json.toJson(applicationConfig))

    s"$messageHeader$configMessage"
  }

}
