import Dependencies._

lazy val defaultSettings = Seq(
  name := "cmwl_pipeline",
  version := "0.1",
  scalaVersion := "2.12.9",
  defaultScalacOptions)

lazy val akkaDependencies = Seq(akkaActor, akkaHttp)
lazy val dbDependencies = Seq(slick)
lazy val testDependencies = Seq(akkaTestKit, scalaCheck, scalaTest)

lazy val pipelines =
  project
    .in(file("."))
    .settings(
      defaultSettings, libraryDependencies ++= akkaDependencies ++ dbDependencies ++ testDependencies
    )

lazy val defaultScalacOptions = scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-Xfatal-warnings"
)