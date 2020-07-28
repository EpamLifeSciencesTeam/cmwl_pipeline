import sbt._

object Dependencies {

  object Version {
    val akka = "2.5.25"
    val akkaHttp = "10.1.10"
    val slick = "3.3.2"
    val slickPg = "0.18.0"
    val hikariCP = "3.3.1"
    val playJson = "2.7.4"
    val akkaHttpJson = "1.29.1"
    val jwtCore = "4.1.0"
    val cats = "2.0.0"
    val scalaCheck = "1.14.0"
    val mockito = "1.10.19"
    val scalaTest = "3.0.8"
    val scalaMock = "4.4.0"
    val tcCore = "0.38.0"
    val tcPostgres = "1.14.3"
    val yaml = "1.23"
    val liquibase = "3.8.6"
    val postgresql = "42.2.8"
    val womtool = "48"
    val logback = "1.2.3"
    val pegdown = "1.6.0"
    val wireMock = "2.26.3"
    val sl4j = "1.7.28"
    val mongo = "2.9.0"
    val mongoCore = "3.12.2"
    val tcMongo = "1.14.3"
  }

  val configHokon = "com.typesafe" % "config" % "1.3.3"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % Version.akkaHttp
  val slick = "com.typesafe.slick" %% "slick" % Version.slick
  val slickPg = "com.github.tminglei" %% "slick-pg" % Version.slickPg
  val slickPgCore = "com.github.tminglei" %% "slick-pg_core" % Version.slickPg
  val hikariCP = "com.typesafe.slick" %% "slick-hikaricp" % Version.hikariCP
  val playJson = "com.typesafe.play" %% "play-json" % Version.playJson
  val playFunctional = "com.typesafe.play" %% "play-functional" % Version.playJson
  val akkaHttpJson = "de.heikoseeberger" %% "akka-http-play-json" % Version.akkaHttpJson
  val jwtCore = "com.pauldijou" %% "jwt-core" % Version.jwtCore
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val catsKernel = "org.typelevel" %% "cats-kernel" % Version.cats
  val scalaMock = "org.scalamock" %% "scalamock" % Version.scalaMock % Test
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test,it"
  val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % "test,it"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck % "test,it"
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test, it"
  val mockito = "org.mockito" % "mockito-all" % Version.mockito % Test
  val tcCore = "com.dimafeng" %% "testcontainers-scala" % Version.tcCore % Test
  val tcPostgres = "org.testcontainers" % "postgresql" % Version.tcPostgres % Test
  val tcMongo = "com.dimafeng" %% "testcontainers-scala-mongodb" % Version.tcCore % Test
  val yaml = "org.yaml" % "snakeyaml" % Version.yaml
  val liquibase = "org.liquibase" % "liquibase-core" % Version.liquibase
  val postgresql = "org.postgresql" % "postgresql" % Version.postgresql
  val womtool = "pipeline" % "womtool" % Version.womtool
  val logback = "ch.qos.logback" % "logback-classic" % Version.logback
  val wireMock = "com.github.tomakehurst" % "wiremock" % Version.wireMock % Test
  val sl4j = "org.slf4j" % "slf4j-api" % Version.sl4j
  val pegdown = "org.pegdown" % "pegdown" % Version.pegdown % Test
  val mongo = "org.mongodb.scala" %% "mongo-scala-driver" % Version.mongo
  val mongoBson = "org.mongodb.scala" %% "mongo-scala-bson" % Version.mongo
  val mongoDriver = "org.mongodb.scala" %% "mongo-scala-driver" % Version.mongo
  val mongoDriverCore = "org.mongodb" % "mongodb-driver-core" % Version.mongoCore
  val bson = "org.mongodb" % "bson" % Version.mongoCore
  val osinka = "com.osinka.subset" %% "subset" % "2.2.3"

  lazy val mongoDependencies = Seq(mongo, mongoBson, mongoDriverCore, bson, mongoDriver, osinka)
  lazy val akkaDependencies = Seq(akkaActor, akkaStreams, akkaHttp)
  lazy val jsonDependencies = Seq(playJson, akkaHttpJson, jwtCore)
  lazy val dbDependencies = Seq(slick, slickPg, hikariCP, postgresql, liquibase, yaml) ++ mongoDependencies
  lazy val coreTestDependencies =
    Seq(mockito, scalaCheck, scalaTest, scalaMock)
  lazy val allTestDependencies =
    coreTestDependencies ++ Seq(akkaTestKit, akkaHttpTestKit, logback, wireMock)
  lazy val testContainers = Seq(tcCore, tcPostgres, tcMongo)
  lazy val cromwellDependencies = Seq(womtool)
}
