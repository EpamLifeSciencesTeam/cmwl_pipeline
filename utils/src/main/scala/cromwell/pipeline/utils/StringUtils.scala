package cromwell.pipeline.utils

import cromwell.pipeline.model.wrapper.Password

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object StringUtils {
  private val encoder = MessageDigest.getInstance("SHA-256")

  def calculatePasswordHash(password: Password, salt: String): String = synchronized {
    val hash = new BigInteger(1, encoder.digest((password.unwrap + salt).getBytes(StandardCharsets.UTF_8)))
    String.format("%032x", hash)
  }
}
