package cromwell.pipeline.datastorage.dao

import cromwell.pipeline.datastorage.Profile
import slick.sql.FixedSqlAction

package object entry {
  trait AliasesSupport { this: Profile =>
    import profile.api._

    type ActionResult[+T] = FixedSqlAction[T, NoStream, Effect.Write]
  }
}
