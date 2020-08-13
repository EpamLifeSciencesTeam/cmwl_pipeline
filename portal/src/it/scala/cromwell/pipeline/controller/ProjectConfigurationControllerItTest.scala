package cromwell.pipeline.controller

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, MongoDBContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.utils.TestContainersUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }
import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import org.mongodb.scala.{ Document, MongoClient }
import com.osinka.subset._
import cromwell.pipeline.datastorage.dao.repository.utils.TestProjectConfigurationUtils
import cromwell.pipeline.datastorage.dto.ProjectConfiguration

class ProjectConfigurationControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  // TODO cc core vs mongo versions
  override def container: MongoDBContainer = TestContainersUtils.getMongoDBContainer()
  implicit lazy val config: Config = TestContainersUtils.getConfigForMongoDBContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.configurationController
  import components.datastorageModule.configurationRepository

  val projectConfiguration = TestProjectConfigurationUtils.projectConfigurationGen.sample.get
  val doc = ProjectConfiguration.toDocument(projectConfiguration)
  val id = projectConfiguration.projectId

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
    val x = configurationRepository.addOne(doc)
  }

}
//  TODO
// import Document repository before tests
// doc. Rep. addOne(DummyProjectConf? id + list of files(none)  ).map(_ => ...)
//  val parsedDoc = ("Hello")
