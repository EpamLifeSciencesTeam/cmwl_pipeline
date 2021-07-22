import sbt._

object Dependencies {

  object Version {
    val akka = "2.6.12"
    val akkaHttp = "10.2.3"
    val akkaHttpCors = "1.1.1"
    val slick = "3.3.3"
    val slickPg = "0.19.4"
    val hikariCP = "3.3.3"
    val playJson = "2.9.1"
    val akkaHttpJson = "1.35.2"
    val jwtCore = "4.2.0"
    val cats = "2.3.0"
    val scalaCheck = "1.14.1"
    val mockito = "1.10.19"
    val scalaTest = "3.0.8"
    val scalaMock = "5.1.0"
    val tcCore = "0.39.3"
    val tcMongodb = "0.39.3"
    val tcPostgres = "1.15.2"
    val yaml = "1.27"
    val liquibase = "4.3.5"
    val postgresql = "42.2.8"
    val logback = "1.2.3"
    val pegdown = "1.6.0"
    val wireMock = "2.27.2"
    val sl4j = "1.7.30"
    val mongo = "4.1.1"
    val mongoCore = "4.1.1"
  }

  val configHokon = "com.typesafe" % "config" % "1.4.0"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % Version.akkaHttp
  val akkaHttpCors = "ch.megard" %% "akka-http-cors" % Version.akkaHttpCors
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
  val tcMongoDB = "com.dimafeng" %% "testcontainers-scala-mongodb" % Version.tcMongodb % Test
  val tcPostgres = "org.testcontainers" % "postgresql" % Version.tcPostgres % Test
  val yaml = "org.yaml" % "snakeyaml" % Version.yaml
  val liquibase = "org.liquibase" % "liquibase-core" % Version.liquibase
  val postgresql = "org.postgresql" % "postgresql" % Version.postgresql
  val logback = "ch.qos.logback" % "logback-classic" % Version.logback
  val wireMock = "com.github.tomakehurst" % "wiremock" % Version.wireMock % Test
  val sl4j = "org.slf4j" % "slf4j-api" % Version.sl4j
  val pegdown = "org.pegdown" % "pegdown" % Version.pegdown % Test
  val mongo = "org.mongodb.scala" %% "mongo-scala-driver" % Version.mongo
  val mongoBson = "org.mongodb.scala" %% "mongo-scala-bson" % Version.mongo
  val mongoDriver = "org.mongodb.scala" %% "mongo-scala-driver" % Version.mongo
  val mongoDriverCore = "org.mongodb" % "mongodb-driver-core" % Version.mongoCore
  val bson = "org.mongodb" % "bson" % Version.mongoCore

  lazy val mongoDependencies = Seq(mongo, mongoBson, mongoDriverCore, bson, mongoDriver)
  lazy val akkaDependencies = Seq(akkaActor, akkaStreams, akkaHttp)
  lazy val jsonDependencies = Seq(playJson, akkaHttpJson, jwtCore)
  lazy val dbDependencies = Seq(slick, slickPg, hikariCP, postgresql, liquibase, yaml) ++ mongoDependencies
  lazy val coreTestDependencies =
    Seq(mockito, scalaCheck, scalaTest, scalaMock)
  lazy val allTestDependencies =
    coreTestDependencies ++ Seq(akkaTestKit, akkaHttpTestKit, logback, wireMock)
  lazy val testContainers = Seq(tcCore, tcPostgres, tcMongoDB)
  lazy val logDependencies = Seq(logback, sl4j)
}
