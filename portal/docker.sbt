defaultLinuxInstallLocation in Docker := "/cmwl_pipeline/app"
packageName in Docker := "cmwl_pipeline"
dockerExposedVolumes := Seq("/cmwl_pipeline/app/logs")
dockerExposedPorts := Seq(8080)
dockerEnvVars := Map(
  "CMWL_DB_SERVER_NAME" -> "172.17.0.1",
  "CMWL_DB_PORT_NUMBER" -> "5432",
  "CMWL_DB_NAME" -> "postgres",
  "CMWL_DB_USER" -> "postgres",
  "CMWL_DB_PASSWORD" -> "docker",
  "CMWL_MONGO_USER" -> "mongoUser",
  "CMWL_MONGO_PASSWORD" -> "password",
  "CMWL_MONGO_HOST" -> "172.17.0.1",
  "CMWL_MONGO_PORT" -> "27017",
  "CMWL_MONGO_AUTH" -> "admin",
  "CMWL_MONGO_DB" -> "mongo",
  "CMWL_MONGO_COLLECTION" -> "configurations",
  "CMWL_GITLAB_USER_NAME" -> "root",
  "CMWL_GITLAB_TOKEN" -> "some gitlab token",
  "CMWL_GITLAB_URL" -> "http://172.17.0.1:9080/api/v4/"
)
