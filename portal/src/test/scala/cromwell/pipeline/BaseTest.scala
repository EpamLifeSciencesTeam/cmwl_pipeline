package cromwell.pipeline

import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait BaseTest extends WordSpec with Matchers with MockFactory  {
  def awaitForResult[T](futureResult: Future[T]): T = Await.result(futureResult, 5.seconds)
}
