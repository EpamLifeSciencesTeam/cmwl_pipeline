package cromwell.pipeline.spec

import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

abstract class ControllerSpec
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockitoSugar
    with PlayJsonSupport
