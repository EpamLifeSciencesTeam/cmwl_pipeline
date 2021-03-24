package cromwell.pipeline.utils

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.Materializer.matFromSystem
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait AkkaTestSources extends BeforeAndAfterAll {
  this: Suite =>
  implicit lazy val actorSystem: ActorSystem = ActorSystem("test")
  implicit lazy val materializer: Materializer = matFromSystem(actorSystem)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    materializer
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }
}
