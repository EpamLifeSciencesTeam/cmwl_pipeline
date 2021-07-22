import Dependencies._

ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.9"
ThisBuild / scalacOptions := Seq(
  "-encoding",
  "utf8",
  "-deprecation",
  "-Xfatal-warnings",
  "-Xlint:unused"
)
ThisBuild / useCoursier := false

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
  test in Test := (test in Test).dependsOn(checkFormat).value,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports"),
  coverageEnabled in Test := true,
  coverageMinimum := 50,
  coverageFailOnMinimum := true,
  coverageExcludedPackages :=
    "cromwell\\.pipeline; cromwell\\.pipeline\\.database.*"
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = (project in file("."))
  .settings(
    name := "Cromwell pipeline",
    commonSettings
  )
  .aggregate(portal, datasource, womtool)

lazy val IntegrationTest = config("it").extend(Test)

lazy val datasource = project
  .settings(
    name := "Datasource",
    commonSettings,
    libraryDependencies ++= dbDependencies ++ logDependencies :+ configHokon :+ mongoBson :+ mongoDriverCore
  )
  .dependsOn(utils)

lazy val portal = project
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "Portal",
    commonSettings,
    libraryDependencies ++= akkaDependencies ++ jsonDependencies ++ logDependencies :+ configHokon :+ akkaHttpCore :+ akkaHttpCors,
    Defaults.itSettings,
    Seq(parallelExecution in Test := false),
    addCommandAlias("testAll", "; test ; it:test"),
    mainClass in Compile := Some("cromwell.pipeline.CromwellPipelineApp")
  )
  .aggregate(repositories, services, controllers, utils)
  .dependsOn(
    datasource,
    repositories % "compile->compile;test->test",
    services,
    controllers,
    utils % "compile->compile;test->test",
    auth
  )

lazy val auth =
  (project in file("auth"))
    .settings(
      libraryDependencies ++= jsonDependencies :+ configHokon :+ playJson :+ akkaHttp,
      commonSettings
    )
    .dependsOn(
      repositories % "compile->compile;test->test",
      utils % "compile->compile;test->test"
    )

lazy val utils =
  (project in file("utils"))
    .configs(IntegrationTest)
    .settings(
      libraryDependencies ++= (jsonDependencies ++ testContainers ++ coreTestDependencies ++ dbDependencies ++ akkaDependencies) :+ configHokon :+ cats :+ playFunctional :+ pegdown,
      commonSettings
    )
    .dependsOn(model)

lazy val repositories =
  (project in file("repositories"))
    .settings(
      Seq(parallelExecution in Test := false),
      libraryDependencies ++= allTestDependencies ++ jsonDependencies ++ mongoDependencies :+ cats :+ slick :+ slickPg :+ slickPgCore :+ configHokon :+ playJson :+ catsKernel :+ playFunctional
    )
    .configs(IntegrationTest)
    .dependsOn(datasource % "compile->compile;test->test", model, utils % "compile->compile;test->test")

lazy val services =
  (project in file("services"))
    .settings(libraryDependencies ++= jsonDependencies ++ mongoDependencies :+ cats :+ playJson)
    .dependsOn(
      repositories % "compile->compile;test->test",
      utils % "compile->compile;test->test",
      womtool,
      model,
      auth
    )

lazy val controllers =
  (project in file("controllers"))
    .settings(
      libraryDependencies ++= akkaDependencies ++ jsonDependencies :+ cats :+ akkaHttpCore :+ playJson
    )
    .dependsOn(services, utils, model, repositories % "test->test")

lazy val womtool = (project in file("womtool"))
  .configs(IntegrationTest)
  .settings(
    name := "WomTool",
    commonSettings,
    libraryDependencies ++= allTestDependencies :+ pegdown :+ configHokon :+ cats,
    addCommandAlias("testAll", "; test ; it:test")
  )
  .dependsOn(repositories)

lazy val model =
  (project in file("model")).settings(libraryDependencies ++= jsonDependencies ++ dbDependencies :+ cats)
