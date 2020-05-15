package cromwell.pipeline.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait AkkaTestSources extends BeforeAndAfterAll {
  this: Suite =>
  implicit lazy val actorSystem = ActorSystem("test")
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    materializer
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }
}
