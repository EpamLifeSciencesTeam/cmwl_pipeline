package cromwell.pipeline.utils

import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.Password
import org.scalacheck.Gen
import org.scalacheck.Gen.alphaNumChar
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.util.concurrent.Executors
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Random

class StringUtilsTest extends WordSpec with ScalaCheckDrivenPropertyChecks with Matchers with TestTimeout {
  private case class SinglePasswordData(pass: Password, salt: String, repeat: Int)
  private case class MultiplePasswordsData(data: Seq[SinglePasswordData])

  private implicit val multiThreadedEc: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  private val maxStringSize = 64
  private val minPasswordRepeats = 64
  private val maxPasswordRepeats = 256
  private val maxPasswordAmount = 64

  private val stringGen = Gen.listOfN(maxStringSize, alphaNumChar).map(_.mkString)
  // fixme temporarily copied from cromwell.pipeline.datastorage.dao.utils.GeneratorUtils
  //  remove once GeneratorUtils moved to utils (duh)
  private val passwordGen: Gen[Password] = for {
    upperCase <- Gen.alphaUpperStr.suchThat(s => s.nonEmpty)
    lowerLetters <- stringGen
    digits <- Gen.posNum[Int]
    symbol <- Gen.oneOf(Seq("$", "%", "&"))
    _ <- Gen.posNum[Long]
  } yield {
    val password = Random.shuffle(List(upperCase, lowerLetters, digits.toString, symbol).flatten).mkString
    Password(password, Enable.Unsafe)
  }

  private val pwdRepeatNumGen = Gen.chooseNum(minPasswordRepeats, maxPasswordRepeats)
  private val singlePasswordDataGen =
    for {
      pass <- passwordGen
      salt <- stringGen
      repeat <- pwdRepeatNumGen
    } yield SinglePasswordData(pass, salt, repeat)
  private val multiplePasswordDataGen = Gen.listOfN(maxPasswordAmount, singlePasswordDataGen).map(MultiplePasswordsData)

  private def await[A](f: Future[A]): A = Await.result(f, timeoutAsDuration)

  private val hashFunction: (Password, String) => String = StringUtils.calculatePasswordHash
  private val hashFunctionF: (Password, String) => Future[String] = (p, s) => Future(hashFunction(p, s))

  "StringUtils" should {
    "not suffer from race conditions" when {
      "pass + hash are the same all the time" in {
        forAll(singlePasswordDataGen) {
          case SinglePasswordData(pass, salt, repeat) =>
            val hashData = Seq.fill(repeat)((pass, salt))
            val actualHashes = await(Future.traverse(hashData)(hashFunctionF.tupled))
            val expectedHash = hashFunction(pass, salt)
            actualHashes should contain only expectedHash
        }
      }

      "pass + hash vary" in {
        forAll(multiplePasswordDataGen) {
          case MultiplePasswordsData(data) =>
            val expectedHashes = data.map { case SinglePasswordData(pass, salt, _) => hashFunction(pass, salt) }

            val actualHashesF = Future.traverse(data) {
              case SinglePasswordData(pass, salt, repeat) =>
                val hashData = Seq.fill(repeat)((pass, salt))
                Future.traverse(hashData)(hashFunctionF.tupled)
            }

            val actualHashes = await(actualHashesF)

            actualHashes.zip(expectedHashes).foreach {
              case (actualHashes, expectedHash) => actualHashes should contain only expectedHash
            }
        }
      }
    }
  }
}
