package eu.regadas

import unfiltered.Cookie
import unfiltered.request.{Cookies, HttpRequest}

case class ClientToken(token: String, user: String) {
  def toCookieString = "%s|%s" format(token, user)
}

object ClientToken {
  def fromCookieString(str: String) = str.split('|') match {
    case Array(v, u) => ClientToken(v, u)
    case ary => sys.error("invalid token cookie string format %s %s" format(str, ary))
  }
}

object CookieToken {
  def unapply[T](r: HttpRequest[T]): Option[ClientToken] = r match {
    case Cookies(cookies) => cookies("token") match {
      case Some(Cookie(_, value, _, _, _, _)) =>
        Some(ClientToken.fromCookieString(value))
      case _ => None
    }
  }
  def apply[T](r: HttpRequest[T]) = unapply(r)

}
