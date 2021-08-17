package cromwell.pipeline.service

import cats.implicits.catsStdShowForString
import cromwell.pipeline.auth.AuthUtils
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.auth._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Password, UserEmail, UserId }
import cromwell.pipeline.service.AuthorizationException.{
  DuplicateUserException,
  InactiveUserException,
  IncorrectPasswordException
}
import cromwell.pipeline.utils.{ AuthConfig, ExpirationTimeInSeconds }
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.{ Matchers, WordSpec }
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim }
import play.api.libs.json.Json

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }

class AuthServiceTest extends WordSpec with Matchers with MockFactory {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val authConfig = AuthConfig(
    secretKey = "secretKey",
    hmacAlgorithm = JwtAlgorithm.fromString(algo = "HS256").asInstanceOf[JwtHmacAlgorithm],
    expirationTimeInSeconds = ExpirationTimeInSeconds(accessToken = 300, refreshToken = 900, userSession = 3600)
  )

  import authConfig._

  private val userId = UserId.random
  private val userPassword = Password("Password_213", Enable.Unsafe)
  private val incorrectUserPassword = Password("Password_2134", Enable.Unsafe)
  private val dummyUser = TestUserUtils.getDummyUserWithCredentials(password = userPassword)
  private val userEmail = dummyUser.email
  private val inactiveUserPassword = Password("Password_213", Enable.Unsafe)
  private val inactiveUser = TestUserUtils.getDummyUserWithCredentials(active = false, password = inactiveUserPassword)
  private val inactiveUserEmail = inactiveUser.email

  "AuthServiceTest" when {

    "refreshTokens" should {

      "return Auth response for active refresh token" taggedAs Service in new RefreshTokenContext(
        expirationTimeInSeconds.refreshToken
      ) {
        authService.refreshTokens(refreshToken) shouldBe Some(authResponse)
      }

      "return None for outdated refresh token" taggedAs Service in new RefreshTokenContext(lifetime = 0) {
        authService.refreshTokens(refreshToken) shouldBe None
      }

      "return None for another type of token" taggedAs Service in new AuthServiceTestContext {
        val currentTimestamp: Long = Instant.now.getEpochSecond
        val accessTokenContent: AuthContent = AccessTokenContent(userId = userId)
        val accessTokenClaims: JwtClaim = JwtClaim(
          content = Json.stringify(Json.toJson(accessTokenContent)),
          expiration = Some(currentTimestamp + expirationTimeInSeconds.accessToken),
          issuedAt = Some(currentTimestamp)
        )
        val accessToken: String = Jwt.encode(accessTokenClaims, secretKey, hmacAlgorithm)

        (authUtils.getOptJwtClaims _ when accessToken).returns(Some(accessTokenClaims))

        authService.refreshTokens(accessToken) shouldBe None
      }

      "return None for wrong token" taggedAs Service in new AuthServiceTestContext {
        val wrongToken = "wrongToken"

        (authUtils.getOptJwtClaims _ when wrongToken).returns(None)

        authService.refreshTokens(wrongToken) shouldBe None
      }
    }

    "signUp" should {

      "return Failed future when user already exists" taggedAs Service in new AuthServiceTestContext {
        (userService.getUserWithCredentialsByEmail _ when userEmail).returns(Future.successful(Some(dummyUser)))
        whenReady(
          authService
            .signUp(
              SignUpRequest(
                dummyUser.email,
                userPassword,
                dummyUser.firstName,
                dummyUser.lastName
              )
            )
            .failed
        ) { _ shouldBe DuplicateUserException(s"$userEmail already exists") }
      }
    }

    "signIn" should {

      "return Failed future when user is inactive" taggedAs Service in new AuthServiceTestContext {
        (userService.getUserWithCredentialsByEmail _ when inactiveUserEmail)
          .returns(Future.successful(Some(inactiveUser)))
        whenReady(
          authService
            .signIn(
              SignInRequest(
                UserEmail(inactiveUserEmail.unwrap, Enable.Unsafe),
                inactiveUserPassword
              )
            )
            .failed
        ) { _ shouldBe InactiveUserException(AuthService.inactiveUserMessage) }
      }

      "return Failed future when password is incorrect" taggedAs Service in new AuthServiceTestContext {
        (userService.getUserWithCredentialsByEmail _ when userEmail).returns(Future.successful(Some(dummyUser)))
        whenReady(
          authService
            .signIn(
              SignInRequest(
                UserEmail(userEmail.unwrap, Enable.Unsafe),
                incorrectUserPassword
              )
            )
            .failed
        ) { _ shouldBe IncorrectPasswordException(AuthService.authorizationFailure) }
      }
    }

    "responseFromUser" should {
      "return whatever getAuthResponse returns" taggedAs Service in new AuthServiceTestContext {
        private val dummyResponse = Some(AuthResponse("", "", 1))
        (authUtils.getAuthResponse _ when (*, *, *)).returns(dummyResponse)
        authService.responseFromUser(dummyUser) shouldBe dummyResponse
      }
    }

    "passwordCorrect" should {
      "return None if a password is correct" taggedAs Service in new AuthServiceTestContext {
        val request: SignInRequest = SignInRequest(
          dummyUser.email,
          userPassword
        )
        authService.passwordCorrect(request, dummyUser) shouldBe None
      }

      "throw the exception if the password is incorrect" taggedAs Service in new AuthServiceTestContext {
        val request: SignInRequest = SignInRequest(
          dummyUser.email,
          incorrectUserPassword
        )
        authService.passwordCorrect(request, dummyUser) shouldBe
          Some(IncorrectPasswordException(AuthService.authorizationFailure))
      }
    }

    "userIsActive" should {
      "return None if a user is active" taggedAs Service in new AuthServiceTestContext {
        authService.userIsActive(dummyUser) shouldBe None
      }

      "throw the exception if a user isn't active" taggedAs Service in new AuthServiceTestContext {
        authService.userIsActive(inactiveUser) shouldBe
          Some(InactiveUserException(AuthService.inactiveUserMessage))
      }
    }
  }

  class AuthServiceTestContext {
    protected val userService: UserService = stub[UserService]
    protected val authUtils: AuthUtils = stub[AuthUtils]
    protected val authService: AuthService = AuthService(userService, authUtils)
  }

  class RefreshTokenContext(lifetime: Long) extends AuthServiceTestContext {
    val currentTimestamp: Long = Instant.now.getEpochSecond
    val refreshTokenContent: AuthContent = RefreshTokenContent(
      userId = userId,
      optRestOfUserSession = Some(expirationTimeInSeconds.userSession - lifetime)
    )
    val refreshTokenClaims: JwtClaim = JwtClaim(
      content = Json.stringify(Json.toJson(refreshTokenContent)),
      expiration = Some(currentTimestamp + lifetime),
      issuedAt = Some(currentTimestamp)
    )
    val refreshToken: String = Jwt.encode(refreshTokenClaims, secretKey, hmacAlgorithm)
    val authResponse: AuthResponse = AuthResponse("accessToken", "refreshToken", expirationTimeInSeconds.accessToken)

    (authUtils.getOptJwtClaims _ when refreshToken).returns(Some(refreshTokenClaims))
    (authUtils.getAuthResponse _ when (*, *, *)).returns(Some(authResponse))
  }

}
