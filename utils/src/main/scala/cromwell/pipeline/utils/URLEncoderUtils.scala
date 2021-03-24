package cromwell.pipeline.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object URLEncoderUtils {

  private val encoding = StandardCharsets.UTF_8.toString

  def encode(url: String): String = URLEncoder.encode(url, encoding)

}
