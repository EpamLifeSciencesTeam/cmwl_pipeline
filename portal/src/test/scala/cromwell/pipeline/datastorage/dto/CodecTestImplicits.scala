package cromwell.pipeline.datastorage.dto

import cats.{ Applicative, Eq }
import org.scalacheck.{ Arbitrary, Gen }
import play.api.libs.json.JsResult
import play.api.libs.json.JsResult.applicativeJsResult

trait CodecTestImplicits {
  implicit def emailGen: Gen[String] = {
    for {
      name <- Gen.alphaStr.filter(s => s.nonEmpty)
      at = "@"
      domain <- Gen.alphaStr.filter(s => s.nonEmpty)
      dotCom = ".com"
    } yield List(name, at, domain, dotCom).mkString
  }

  implicit val userArbitrary: Arbitrary[User] = Arbitrary {
    for {
      userId <- Gen.identifier
      email <- emailGen
      passwordHash <- Gen.alphaStr
      passwordSalt <- Gen.alphaStr
      firstName <- Gen.alphaStr
      lastName <- Gen.alphaStr
    } yield User(
      UserId(userId),
      email,
      passwordHash,
      passwordSalt,
      firstName,
      lastName
    )
  }

  implicit val jsResultApplicative: Applicative[JsResult] = new Applicative[JsResult] {
    override def pure[A](x: A): JsResult[A] = applicativeJsResult.pure(x)

    override def ap[A, B](ff: JsResult[A => B])(fa: JsResult[A]): JsResult[B] = applicativeJsResult.apply(ff, fa)
  }

  implicit def eq[A]: Eq[A] = Eq.fromUniversalEquals
}
