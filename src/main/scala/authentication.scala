package eu.regadas

import unfiltered._
import unfiltered.netty._
import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie
import jira._
import java.net.URL
import com.weiglewilczek.slf4s._

case class ClientToken(token: String, user: String) {
  def toCookieString = "%s|%s" format (token, user)
}

object ClientToken {
  def fromCookieString(str: String) = str.split('|') match {
    case Array(v, u) => ClientToken(v, u)
    case ary => sys.error("invalid token cookie string format %s %s" format (str, ary))
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

trait Authentication {

  def auth[T](request: HttpRequest[T])(f: ClientToken => ResponseFunction[Any]): ResponseFunction[Any]  = try {
    CookieToken(request) match {
      case Some(rt) => f(rt)
      case _ => Forbidden
    }
  } catch {
    case _ => 
      ResponseCookies(Cookie("token", "", maxAge = Some(0))) ~> Redirect("/")
  }
}

object Authentication extends cycle.Plan with cycle.SynchronousExecution with JiraWorkAholicErrorResponse with Template with Logging {

  object User extends Params.Extract("user", Params.first ~> Params.nonempty)
  object Password extends Params.Extract("password", Params.first ~> Params.nonempty)

  lazy val api = Api(new URL(Props.get("JIRA_WS")))

  def intent: Cycle.Intent[Any, Any] = {
    case req @ Path("/login") => req match {
      case GET(_) => 
        ResponseCookies(Cookie("token", "", maxAge = Some(0))) ~> login
      case POST(_ & Params(User(user) & Password(password))) => try {
          ResponseCookies(Cookie("token", api.login(user, password).toCookieString)) ~> Redirect("/")
        } catch { case _ => Redirect("/") }
    }
    case req @ GET(Path("/logout")) => {
      CookieToken(req) match {
        case Some(rt) =>
          api.logout(rt)
        case _ => Forbidden
      }
      ResponseCookies(Cookie("token", "", maxAge = Some(0))) ~> Redirect("/")
    }
  }

}
