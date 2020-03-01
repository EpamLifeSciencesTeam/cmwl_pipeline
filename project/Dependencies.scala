import sbt._

object Dependencies {

  object Version {
    val akka = "2.5.25"
    val akkaHttp = "10.1.10"
    val slick = "3.3.2"
    val hikariCP = "3.3.1"
    val playJson = "2.7.3"
    val akkaHttpJson = "1.29.1"
    val jwtCore = "4.1.0"
    val cats = "2.0.0"
    val scalaCheck = "1.14.0"
    val mockito = "1.10.19"
    val scalaTest = "3.0.8"
    val scalaMock = "4.4.0"
    val tcCore = "0.34.0"
    val tcPostgres = "1.12.2"
    val yaml = "1.23"
    val liquibase = "3.8.6"
    val postgresql = "42.2.8"
    val womtool = "48"
    val logback = "1.2.3"
  }

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val slick = "com.typesafe.slick" %% "slick" % Version.slick
  val hikariCP = "com.typesafe.slick" %% "slick-hikaricp" % Version.hikariCP
  val playJson = "com.typesafe.play" %% "play-json" % Version.playJson
  val akkaHttpJson = "de.heikoseeberger" %% "akka-http-play-json" % Version.akkaHttpJson
  val jwtCore = "com.pauldijou" %% "jwt-core" % Version.jwtCore
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val scalaMock = "org.scalamock" %% "scalamock" % Version.scalaMock % Test
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test,it"
  val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % "test,it"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck % "test,it"
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test,it"
  val mockito = "org.mockito" % "mockito-all" % Version.mockito % Test
  val tcCore = "com.dimafeng" %% "testcontainers-scala" % Version.tcCore % Test
  val tcPostgres = "org.testcontainers" % "postgresql" % Version.tcPostgres % Test
  val yaml = "org.yaml" % "snakeyaml" % Version.yaml
  val liquibase = "org.liquibase" % "liquibase-core" % Version.liquibase
  val postgresql = "org.postgresql" % "postgresql" % Version.postgresql
  val womtool = "pipeline" % "womtool" % Version.womtool
  val logback = "ch.qos.logback" % "logback-classic" % Version.logback

  lazy val akkaDependencies = Seq(akkaActor, akkaStreams, akkaHttp, logback)
  lazy val jsonDependencies = Seq(playJson, akkaHttpJson, jwtCore)
  lazy val dbDependencies = Seq(slick, hikariCP, postgresql, liquibase, yaml)
  lazy val testDependencies = Seq(mockito, akkaTestKit, akkaHttpTestKit, scalaCheck, scalaTest, scalaMock)
  lazy val testContainers = Seq(tcCore, tcPostgres)
  lazy val cromwellDependencies = Seq(womtool)
}
