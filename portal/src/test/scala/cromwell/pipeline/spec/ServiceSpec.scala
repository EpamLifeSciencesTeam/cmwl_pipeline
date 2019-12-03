package cromwell.pipeline.spec

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

abstract class ServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with ScalaFutures
