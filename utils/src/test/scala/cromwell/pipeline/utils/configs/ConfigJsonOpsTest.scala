package cromwell.pipeline.utils.configs

import com.typesafe.config.{ Config, ConfigFactory }
import cromwell.pipeline.utils.ApplicationConfig
import org.scalatest.Matchers._
import org.scalatest.WordSpec

class ConfigJsonOpsTest extends WordSpec {

  val sourceConfigWithPasswordsFilled: String =
    s"""webservice {
       |  interface = "wsInterface"
       |  port = 4040
       |  cors {
       |    allowedOrigins = [ "http://localhost:3000" ]
       |  }
       |}
       |auth {
       |  secretKey = "authSecretKey"
       |  hmacAlgorithm = "HS256"
       |  expirationTimeInSeconds {
       |    accessToken = 10
       |    refreshToken = 20
       |    userSession = 30
       |  }
       |}
       |database {
       |  postgres_dc {
       |    profile = "dbProfile"
       |    db {
       |      properties {
       |        serverName = "postgreServName"
       |        portNumber = 8080
       |        databaseName = "postgre"
       |        user = "postgreUser"
       |        password = "123qwerty"
       |      }
       |      numThreads = 10
       |    }
       |  }
       |  gitlab {
       |    url = "gitlab.com"
       |    token = "secretToken"
       |    defaultFileVersion = "0.0.1"
       |    defaultBranch = "gitLabBranch"
       |  }
       |  mongo {
       |    user = "mongoUser"
       |    password = "124qwertymongo"
       |    host = "mongohost"
       |    port = 1000
       |    authenticationDatabase = "mongoAuthBase"
       |    database = "mongoDB"
       |    collection = "mongoCollection"
       |  }
       |}""".stripMargin

  val sourceConfigWithPasswordsEmpty: String =
    s"""webservice {
       |  interface = "wsInterface"
       |  port = 4040
       |  cors {
       |    allowedOrigins = [ "http://localhost:3000" ]
       |  }
       |}
       |auth {
       |  secretKey = ""
       |  hmacAlgorithm = "HS256"
       |  expirationTimeInSeconds {
       |    accessToken = 10
       |    refreshToken = 20
       |    userSession = 30
       |  }
       |}
       |database {
       |  postgres_dc {
       |    profile = "dbProfile"
       |    db {
       |      properties {
       |        serverName = "postgreServName"
       |        portNumber = 8080
       |        databaseName = "postgre"
       |        user = "postgreUser"
       |        password = ""
       |      }
       |      numThreads = 10
       |    }
       |  }
       |  gitlab {
       |    url = "gitlab.com"
       |    token = ""
       |    defaultFileVersion = "0.0.1"
       |    defaultBranch = "gitLabBranch"
       |  }
       |  mongo {
       |    user = "mongoUser"
       |    password = ""
       |    host = "mongohost"
       |    port = 1000
       |    authenticationDatabase = "mongoAuthBase"
       |    database = "mongoDB"
       |    collection = "mongoCollection"
       |  }
       |}""".stripMargin

  val targetMessageWithPasswordsFilled: String =
    s"""
       |----------------------------------------------------
       |Application starts with the following configuration:
       |----------------------------------------------------
       |{
       |  "WebServiceConfig" : {
       |    "interface" : "wsInterface",
       |    "port" : 4040,
       |    "cors" : {
       |      "allowedOrigins" : [ "http://localhost:3000" ]
       |    }
       |  },
       |  "AuthConfig" : {
       |    "secretKey" : "*********",
       |    "hmacAlgorithm" : "HS256",
       |    "expirationTimeInSeconds" : {
       |      "accessToken" : 10,
       |      "refreshToken" : 20,
       |      "userSession" : 30
       |    }
       |  },
       |  "GitLabConfig" : {
       |    "url" : "gitlab.com",
       |    "token" : {
       |      "PRIVATE-TOKEN" : "*********"
       |    },
       |    "defaultFileVersion" : "0.0.1",
       |    "defaultBranch" : "gitLabBranch"
       |  },
       |  "MongoConfig" : {
       |    "user" : "mongoUser",
       |    "password" : "*********",
       |    "host" : "mongohost",
       |    "port" : 1000,
       |    "authenticationDatabase" : "mongoAuthBase",
       |    "database" : "mongoDB",
       |    "collection" : "mongoCollection"
       |  },
       |  "PostgreConfig" : {
       |    "serverName" : "postgreServName",
       |    "portNumber" : 8080,
       |    "databaseName" : "postgre",
       |    "user" : "postgreUser",
       |    "password" : "*********"
       |  }
       |}""".stripMargin

  val targetMessageWithPasswordsEmpty: String =
    s"""
       |----------------------------------------------------
       |Application starts with the following configuration:
       |----------------------------------------------------
       |{
       |  "WebServiceConfig" : {
       |    "interface" : "wsInterface",
       |    "port" : 4040,
       |    "cors" : {
       |      "allowedOrigins" : [ "http://localhost:3000" ]
       |    }
       |  },
       |  "AuthConfig" : {
       |    "secretKey" : "",
       |    "hmacAlgorithm" : "HS256",
       |    "expirationTimeInSeconds" : {
       |      "accessToken" : 10,
       |      "refreshToken" : 20,
       |      "userSession" : 30
       |    }
       |  },
       |  "GitLabConfig" : {
       |    "url" : "gitlab.com",
       |    "token" : {
       |      "PRIVATE-TOKEN" : ""
       |    },
       |    "defaultFileVersion" : "0.0.1",
       |    "defaultBranch" : "gitLabBranch"
       |  },
       |  "MongoConfig" : {
       |    "user" : "mongoUser",
       |    "password" : "",
       |    "host" : "mongohost",
       |    "port" : 1000,
       |    "authenticationDatabase" : "mongoAuthBase",
       |    "database" : "mongoDB",
       |    "collection" : "mongoCollection"
       |  },
       |  "PostgreConfig" : {
       |    "serverName" : "postgreServName",
       |    "portNumber" : 8080,
       |    "databaseName" : "postgre",
       |    "user" : "postgreUser",
       |    "password" : ""
       |  }
       |}""".stripMargin

  "ConfigJsonOps" when {
    "generating config message with sensitive data" should {
      "replace filled sensitive data with *********" in {

        val testConfigWithPasswordsFilled: Config = ConfigFactory.parseString(sourceConfigWithPasswordsFilled)
        val testApplicationConfigWithPasswordsFilled = new ApplicationConfig(testConfigWithPasswordsFilled)

        targetMessageWithPasswordsFilled shouldEqual ConfigJsonOps.configToJsonString(
          testApplicationConfigWithPasswordsFilled
        )
      }
      "leave empty sensitive data as is" in {

        val testConfigWithPasswordsEmpty: Config = ConfigFactory.parseString(sourceConfigWithPasswordsEmpty)
        val testApplicationConfigWithPasswordsEmpty = new ApplicationConfig(testConfigWithPasswordsEmpty)

        targetMessageWithPasswordsEmpty shouldEqual ConfigJsonOps.configToJsonString(
          testApplicationConfigWithPasswordsEmpty
        )
      }
    }
  }
}
