package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.utils.golden.CodecTests
import org.scalatest.FunSuiteLike
import org.scalatest.prop.Configuration

class UserCodecTests
  extends FunSuiteLike
    with FunSuiteDiscipline
    with Configuration
    with CodecTestImplicits {

  checkAll("UserCodecTests", CodecTests[User].tests)
}
