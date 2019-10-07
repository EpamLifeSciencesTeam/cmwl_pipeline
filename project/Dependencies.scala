import sbt._

object Dependencies {

  object Version {
    val akka        = "2.5.25"
    val akkaHttp    = "10.1.9"
    val slick       = "3.3.2"
    val scalaCheck  = "1.14.0"
    val scalaTest   = "3.0.8"
    val macwire     = "2.3.3"
    val liquibase   = "3.8.0"
    val postgresql  = "42.2.8"
    val hikariCP    = "3.3.1"
  }


  val akkaActor   = "com.typesafe.akka"        %% "akka-actor"     % Version.akka
  val akkaHttp    = "com.typesafe.akka"        %% "akka-http"      % Version.akkaHttp
  val slick       = "com.typesafe.slick"       %% "slick"          % Version.slick
  val hikariCP    = "com.typesafe.slick"       %% "slick-hikaricp" % Version.hikariCP
  val akkaTestKit = "com.typesafe.akka"        %% "akka-testkit"   % Version.akka       % Test
  val scalaCheck  = "org.scalacheck"           %% "scalacheck"     % Version.scalaCheck % Test
  val scalaTest   = "org.scalatest"            %% "scalatest"      % Version.scalaTest  % Test
  val macwire     = "com.softwaremill.macwire" %% "macros"         % Version.macwire    % Provided
  val liquibase   = "org.liquibase"             % "liquibase-core" % Version.liquibase
  val postgresql  = "org.postgresql"            % "postgresql"     % Version.postgresql


  lazy val akkaDependencies = Seq(akkaActor, akkaHttp)
  lazy val dbDependencies   = Seq(slick, hikariCP, liquibase, postgresql)
  lazy val testDependencies = Seq(akkaTestKit, scalaCheck, scalaTest)
}
