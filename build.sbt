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
  test in Test := (test in Test).dependsOn(checkFormat).value,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports")
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
    libraryDependencies ++= dbDependencies :+ configHokon
  )
  .dependsOn(utils)

lazy val portal = project
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "Portal",
    commonSettings,
    libraryDependencies ++= akkaDependencies ++ jsonDependencies ++ cromwellDependencies :+ configHokon :+ akkaHttpCore :+ sl4j,
    Defaults.itSettings,
    Seq(parallelExecution in Test := false),
    mappings in Universal ++= Seq(
      (resourceDirectory in Compile).value / "application.conf" -> "conf/application.conf",
      (resourceDirectory in Compile).value / "logback.xml" -> "conf/logback.xml"
    ),
    addCommandAlias("testAll", "; test ; it:test")
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
      libraryDependencies ++= (jsonDependencies ++ testContainers ++ coreTestDependencies) :+ configHokon :+ cats :+ playFunctional :+ pegdown,
      commonSettings
    )
    .dependsOn(model)

lazy val repositories =
  (project in file("repositories"))
    .settings(
      libraryDependencies ++= allTestDependencies ++ jsonDependencies :+ cats :+ slick :+ slickPg :+ slickPgCore :+ configHokon :+ playJson :+ catsKernel :+ playFunctional
    )
    .configs(IntegrationTest)
    .dependsOn(datasource, model, utils % "compile->compile;test->test")

lazy val services =
  (project in file("services"))
    .settings(libraryDependencies ++= jsonDependencies ++ cromwellDependencies :+ cats :+ playJson)
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
      libraryDependencies ++= akkaDependencies ++ jsonDependencies ++ cromwellDependencies :+ cats :+ akkaHttpCore :+ playJson
    )
    .dependsOn(services, utils, model, repositories % "test->test")

lazy val womtool = (project in file("womtool"))
  .configs(IntegrationTest)
  .settings(
    resolvers += Resolver.bintrayRepo("scalalab", "pipeline"),
    name := "WomTool",
    commonSettings,
    libraryDependencies ++= allTestDependencies ++ cromwellDependencies :+ pegdown,
    addCommandAlias("testAll", "; test ; it:test")
  )

lazy val model =
  (project in file("model")).settings(libraryDependencies ++= jsonDependencies ++ dbDependencies :+ cats)
