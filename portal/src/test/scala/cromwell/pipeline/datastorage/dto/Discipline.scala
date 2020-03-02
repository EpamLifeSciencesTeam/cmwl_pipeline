package cromwell.pipeline.datastorage.dto

import org.scalactic.Prettifier
import org.scalactic.source.Position
import org.scalatest.prop.Configuration
import org.scalatest.FunSuiteLike
import org.scalatestplus.scalacheck.{CheckerAsserting, Checkers}
import org.typelevel.discipline.Laws

trait Discipline extends Checkers { self: Configuration =>
  protected val config: PropertyCheckConfiguration = PropertyCheckConfiguration()

  def checkAll(name: String, ruleSet: Laws#RuleSet)(implicit config: PropertyCheckConfiguration,
                                                    prettifier: Prettifier,
                                                    pos: Position): Unit

}

trait FunSuiteDiscipline extends Discipline { self: FunSuiteLike with Configuration =>
  final def checkAll(name: String, ruleSet: Laws#RuleSet)(implicit config: PropertyCheckConfiguration,
                                                          prettifier: Prettifier,
                                                          pos: Position): Unit =
    for ((id, prop) <- ruleSet.all.properties)
      test(s"$name.$id") {
        check(prop)(config, asserting = CheckerAsserting.assertingNatureOfAssertion, prettifier, pos)
      }
}


