import Dependencies._

ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.9"
ThisBuild / scalacOptions := Seq(
  "-encoding",
  "utf8",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val formatAll = taskKey[Unit]("Format all the source code which includes src, test, and build files")
lazy val checkFormat = taskKey[Unit]("Check all the source code which includes src, test, and build files")

lazy val commonSettings = Seq(
  formatAll := {
    (scalafmt in Compile).value
    (scalafmt in Test).value
  },
  checkFormat := {
    (scalafmtCheck in Compile).value
    (scalafmtCheck in Test).value
  },
  compile in Compile := (compile in Compile).dependsOn(checkFormat).value,
  test in Test := (test in Test).dependsOn(checkFormat).value
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = (project in file("."))
  .settings(
    unmanagedJars in Compile += file(Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/wdltool-0.14.jar"),
    unmanagedJars in Compile += file(Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/womtool-48-787cf8b-SNAP.jar"),
    name := "Cromwell pipeline",
    commonSettings
  )
  .aggregate(portal, datasource)

lazy val IntegrationTest = config("it").extend(Test)

lazy val datasource = project.settings(
  name := "Datasource",
  commonSettings,
  libraryDependencies ++= dbDependencies
)

lazy val portal = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= akkaDependencies ++ testDependencies ++ jsonDependencies ++ macwire ++ testContainers,
    libraryDependencies += cats,
    //TODO need to check out parallel execution
    addCommandAlias("testAll", "; test ; it:test")
  )
  .dependsOn(datasource)

lazy val womtool = (project in file("."))
  .settings(
//    unmanagedJars in Compile += file(Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/womtool-48.jar"),
    unmanagedJars in Compile += file("./lib/womtool-48.jar"),
//    unmanagedJars in Compile += file(Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/wdltool-0.14.jar"),
//    unmanagedJars in Compile += file(Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/womtool-48-787cf8b-SNAP.jar"),
//    unmanagedJars in Compile += file(
//      Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/womtool_2.12-48-787cf8b-SNAP-javadoc"
//    ),
//    unmanagedJars in Compile += file(
//      Path.userHome + "/projects/cmwlppl/cmwl_pipeline/lib/wdl-biscayne_2.12-48-787cf8b-SNAP-javadoc.jar"
//    ),
    name := "womTool"
  )
  .dependsOn()
