import Dependencies._

ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.9"
ThisBuild / scalacOptions := Seq(
    "-encoding", "utf8",
    "-deprecation",
    "-Xfatal-warnings"
)

lazy val root = (project in file("."))
    .settings(name := "Cromwell pipeline")
    .aggregate(portal)


lazy val datasource = project.settings(
    name := "Datasource",
    libraryDependencies ++= dbDependencies)

lazy val portal = project
    .settings(
        name := "Portal",
        libraryDependencies ++= akkaDependencies ++ testDependencies :+ macwire,
    ).dependsOn(datasource)
