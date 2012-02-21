package eu.regadas
import com.weiglewilczek.slf4s.Logger

object Clock {
  import System.{ currentTimeMillis => now }
  def apply[T](what: String)(f: => T)(implicit logger: Logger) = {
    val then = now
    try { f }
    finally {
      logger.debug("%s took %d ms" format (what, now - then))
    }
  }
}