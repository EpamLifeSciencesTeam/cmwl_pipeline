package cromwell.pipeline.database

object LiquibaseExceptions {
  final case class LiquibaseUpdateSchemaException(
    cause: Throwable,
    message: String = "An error during the schema update via liquibase."
  ) extends RuntimeException(message, cause)
}
