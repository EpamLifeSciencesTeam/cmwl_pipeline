import sbt._

object Dependencies {

  object Version {
    val akkaActor = "2.5.25"

    val akkaHttp = "10.1.9"

    val slick = "3.3.2"

    val akkaTestKit = "2.5.25"

    val scalaCheck = "1.14.0"

    val scalaTest = "3.0.8"
  }

  val akkaActor   = "com.typesafe.akka"  %% "akka-actor"   % Version.akkaActor
  val akkaHttp    = "com.typesafe.akka"  %% "akka-http"    % Version.akkaHttp
  val slick       = "com.typesafe.slick" %% "slick"        % Version.slick
  val akkaTestKit = "com.typesafe.akka"  %% "akka-testkit" % Version.akkaTestKit
  val scalaCheck  = "org.scalacheck"     %% "scalacheck"   % Version.scalaCheck % Test
  val scalaTest   = "org.scalatest"      %% "scalatest"    % Version.scalaTest % Test


  lazy val akkaDependencies = Seq(akkaActor, akkaHttp)
  lazy val dbDependencies = Seq(slick)
  lazy val testDependencies = Seq(akkaTestKit, scalaCheck, scalaTest)
}
