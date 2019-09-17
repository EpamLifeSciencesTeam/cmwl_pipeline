import Dependencies._

lazy val defaultSettings = Seq(
  name := "cromwell_pipeline",
  version := "0.1",
  scalaVersion := "2.12.9",
  defaultScalacOptions)

lazy val cromwell_pipeline = (project in file("."))
    .settings(
      defaultSettings, 
      libraryDependencies ++= akkaDependencies ++ dbDependencies ++ testDependencies
    )

lazy val defaultScalacOptions = scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-Xfatal-warnings"
)