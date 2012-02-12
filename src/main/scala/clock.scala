package eu.regadas
import com.weiglewilczek.slf4s.Logger

object Clock {
  import System.{ currentTimeMillis => now }
  def apply[T](what: String, l: Logger)(f: => T) = {
    val then = now
    try { f }
    finally {
      l.debug("%s took %d ms" format (what, now - then))
    }
  }
}