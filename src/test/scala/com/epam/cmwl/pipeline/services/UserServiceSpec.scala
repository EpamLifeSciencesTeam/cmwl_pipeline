package com.epam.cmwl.pipeline.services

import org.scalatest.{FreeSpec, Matchers}

class UserServiceSpec extends FreeSpec with Matchers{

    "UserService should run one test" in {
      UserService.test shouldBe 1
    }
}
