package eu.regadas

import unfiltered._
import unfiltered.netty._
import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie
import jira._
import java.net.URL
import net.liftweb.json.JsonDSL._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import com.weiglewilczek.slf4s._
import net.liftweb.json.JsonAST.JObject

object Browser {
  /** Paths for which we care not */
  def trapdoor: Cycle.Intent[Any, Any] = {
    case GET(Path("/favicon.ico")) => NotFound
  }
}

object JiraWorkAholic extends cycle.Plan with cycle.SynchronousExecution with JiraWorkAholicErrorResponse with Template with Logging {

  import QParams._

  object User extends Params.Extract("user", Params.first ~> Params.nonempty)
  object Password extends Params.Extract("password", Params.first ~> Params.nonempty)
  object WorkLog extends Params.Extract("worklog", Params.first ~> Params.nonempty)

  def intent = authentication orElse home orElse projects orElse issues orElse cache

  lazy val api = Api(new URL(Props.get("JIRA_WS")))

  def home: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/") & Cookies(c)) => CookieToken(req) match {
      case Some(rt) => home(
        <p class="navbar-text pull-right">Logged in as <a href="#">{ rt.user }</a> <a href="/logout">Logout</a></p>)(
          <li class="nav-header">Projects</li>
          <div>
            {
              api.project.list(rt) map { p =>
                <li class="project" data-key={ p.key }>
                  <i class="icon-plus-sign"/>{ p.name }<a href={ p.url }>[Jira Link]</a>
                  <ul style="display: none;"></ul>
                </li>
              }
            }
          </div>)(<div class="row-fluid">
                    <div id="calendar"></div>
                    <div id="myModal" class="modal fade" style="display: none;">
                      <div class="modal-header">
                        <a class="close" data-dismiss="modal">×</a>
                        <h3>Add Issue WorkLog</h3>
                      </div>
                      <div class="modal-body">
                        <fieldset>
                          <div class="control-group">
                            <label for="select01" class="control-label">Start Time</label>
                            <div class="controls">
                              <input type="text" class="time ui-timepicker-input" id="start-event" autocomplete="off"/>
                            </div>
                          </div>
                          <div class="control-group">
                            <label for="select02" class="control-label">End Time</label>
                            <div class="controls">
                              <input type="text" class="time ui-timepicker-input" id="end-event" autocomplete="off"/>
                            </div>
                          </div>
                        </fieldset>
                      </div>
                      <div class="modal-footer">
                        <a id="add" href="#" class="btn btn-primary">Add</a>
                        <a href="#" class="btn" data-dismiss="modal">Close</a>
                      </div>
                    </div>
                  </div>)
      case _ => index
    }

  }

  def authentication: Cycle.Intent[Any, Any] = {
    case POST(Path("/login") & Params(User(user) & Password(password))) => {
      try {

        Db.collection("users") { c =>

        }

        ResponseCookies(Cookie("token", api.login(user, password).toCookieString)) ~> Redirect("/")
      } catch { case _ => Redirect("/") }
    }

    case GET(Path("/logout")) =>
      ResponseCookies(Cookie("token", "", maxAge = Some(0))) ~> Redirect("/")
  }

  def projects: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/projects")) => CookieToken(req) match {
      case Some(rt) =>
        JsonContent ~> Json(api.project.list(rt))
      case _ => Forbidden
    }
    case req @ GET(Path(Seg("projects" :: project :: "issues" :: Nil)) & Params(params)) => CookieToken(req) match {
      case Some(rt) =>
        val expected = for {
          max <- lookup("max") is
            int { _ + " is not an integer" }
        } yield JsonContent ~> Json(api.issue.list(rt, project, max))
        expected(params) orFail { fails => BadRequest }
      case _ => Forbidden
    }
    case req @ GET(Path(Seg("projects" :: project :: "worklog" :: Nil)) & Params(params)) => CookieToken(req) match {
      case Some(rt) =>
        val expected = for {
          max <- lookup("max") is
            int { _ + " is not an integer" }
        } yield JsonContent ~> Json(api.project.worklogs(rt, project, max))
        expected(params) orFail { fails => BadRequest }
      case _ => Forbidden
    }
  }

  def issues: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("projects" :: project :: "issues" :: issue :: "worklog" :: Nil))) => CookieToken(req) match {
      case Some(rt) => JsonContent ~> Json(api.issue.worklogs(rt, project, issue))
      case _ => Forbidden
    }
    case req @ POST(Path(Seg("projects" :: project :: "issues" :: issue :: "worklog" :: Nil))) => CookieToken(req) match {
      case Some(rt) =>
        import net.liftweb.json._
        val json = parse(Body.string(req))
        for {
          JField("created", JInt(createdTime)) <- json
          JField("start", JInt(startTime)) <- json
          JField("end", JInt(endTime)) <- json
        } yield {
          val start = new DateTime(startTime.toLong)
          val created = new DateTime(createdTime.toLong)
          model.WorkLog(None, project, issue, (endTime - startTime).toLong / 1000, start, created).cache(rt.user)
        }
        JsonContent ~> Ok
      case _ => Forbidden
    }
  }

  def cache: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("cached" :: project :: "worklog" :: Nil))) => CookieToken(req) match {
      case Some(rt) => JsonContent ~> Json(model.WorkLog.cachedByProject(rt.user, project).toList)
      case _ => Forbidden
    }
  }
}