package eu.regadas

import unfiltered._
import unfiltered.netty._
import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie
import model._
import jira._
import java.net.URL
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import com.weiglewilczek.slf4s._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object Site extends cycle.Plan with cycle.SynchronousExecution with JiraWorkAholicErrorResponse with Template with Logging {

  import QParams._

  implicit lazy val api = Api(new URL(Props.get("JIRA_WS")))

  def intent = (home /: Seq(projects, issues, worklogs, update, user, search))(_ orElse _)

  def home: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/") & Cookies(c)) => CookieToken(req) match {
      case Some(rt) => home(
        <p class="navbar-text pull-right">Logged in as <a href="#">{ rt.user }</a> <a href="/logout">Logout</a></p>)(
          <li class="nav-header">Favorite Issues</li>
          <ul id="fav-issues" class="nav nav-list"><li class="empty"><p>Drop here some issues</p></li></ul>
          <li class="nav-header">Search Issues</li>
          <ul class="nav nav-list">
            <form id="q-form" class="form-search">
              <input id="q" type="text" class="input-medium search-query" placeholder="Search for Issues ..."/>
              <div id="search-spinner"/>
            </form>
            <div id="results"><ul class="nav nav-list"/></div>
          </ul>
          <li class="nav-header">All Projects</li>
          <ul class="nav nav-list">
            {
              api.project.list(rt) map { p =>
                <li class="project" data-key={ p.key }>
                  <a href="#">
                    <i class="icon-plus-sign"/>{ p.name }
                  </a>
                  <ul></ul>
                </li>
              }
            }
          </ul>)(<div class="row-fluid">
                   <div id="work-spinner"/>
                   <div id="messages"></div>
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

  def search: Cycle.Intent[Any, Any] = {
    //FIXME: some problem in unfiltered with param extractor ....
    case req @ POST(Path("/search/issues") & Params(params)) => CookieToken(req) match {
      case Some(rt) =>
        val expected = for {
          query <- lookup("query") is
            nonempty("query is empty") is
            required("missing query")
        } yield JsonContent ~> Json(api.issue.search(rt, query.get))
        expected(params) orFail { fails => BadRequest }
      case _ => Forbidden ~> Redirect("/")
    }
  }

  def projects: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/projects")) => CookieToken(req) match {
      case Some(rt) =>
        JsonContent ~> Json(api.project.list(rt))
      case _ => Forbidden ~> Redirect("/")
    }
  }

  def issues: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("projects" :: project :: "issues" :: Nil)) & Params(params)) => CookieToken(req) match {
      case Some(rt) =>
        val expected = for {
          max <- lookup("max") is
            int { _ + " is not an integer" }
        } yield JsonContent ~> Json(api.issue.list(rt, project, max))
        expected(params) orFail { fails => BadRequest }
      case _ => Forbidden ~> Redirect("/")
    }
  }

  def worklogs: Cycle.Intent[Any, Any] = {
    case req @ GET(Path(Seg("projects" :: project :: "worklog" :: Nil)) & Params(params)) => CookieToken(req) match {
      case Some(rt) =>
        val expected = for {
          max <- lookup("max") is
            int { _ + " is not an integer" }
        } yield JsonContent ~> Json(api.project.worklogs(rt, project, max))
        expected(params) orFail { fails => BadRequest }
      case _ => Forbidden ~> Redirect("/")
    }
    case req @ GET(Path(Seg("projects" :: project :: "issues" :: issue :: "worklog" :: Nil))) => CookieToken(req) match {
      case Some(rt) => JsonContent ~> Json(api.issue.worklogs(rt, project, issue))
      case _ => Forbidden ~> Redirect("/")
    }
  }

  def update: Cycle.Intent[Any, Any] = {
    case req @ POST(Path(Seg("projects" :: project :: "issues" :: issue :: "worklog" :: Nil))) => CookieToken(req) match {
      case Some(rt) =>
        val json = parse(Body.string(req))
        for {
          JField("created", JInt(createdTime)) <- json
          JField("start", JInt(startTime)) <- json
          JField("end", JInt(endTime)) <- json
        } yield {
          val start = new DateTime(startTime.toLong)
          val created = new DateTime(createdTime.toLong)
          model.WorkLog(None, project, issue, (endTime - startTime).toLong / 1000, start, created).save(rt.user)
        }
        Ok
      case _ => Forbidden ~> Redirect("/")
    }
    case req @ POST(Path(Seg("projects" :: project :: "issues" :: issue :: "worklog" :: "delete" :: Nil))) => CookieToken(req) match {
      case Some(rt) =>
        val json = parse(Body.string(req))
        for {
          JField("created", JInt(createdTime)) <- json
        } yield model.WorkLog.remove(rt.user, project, issue, new DateTime(createdTime.toLong))
        JsonContent ~> Ok
      case _ => Forbidden ~> Redirect("/")
    }
  }

  def user: Cycle.Intent[Any, Any] = {
    case req @ Path("/user/favorite/issues") => CookieToken(req) match {
      case Some(rt) => req match {
        case GET(_) => JsonContent ~> Json(model.Issue.list(rt.user).toList)
        case POST(_) =>
          val json = parse(Body.string(req))
          for {
            JField("key", JString(key)) <- json
            JField("summary", JString(summary)) <- json
          } yield {
            model.Issue(key, summary).save(rt.user)
          }
          Ok
        case _ => NotFound
      }
      case _ => Forbidden ~> Redirect("/")

    }

    case req @ GET(Path(Seg("state" :: Nil))) => CookieToken(req) match {
      case Some(rt) => JsonContent ~> Json(("cache" -> model.User.hasState(rt.user)))
      case _ => Forbidden ~> Redirect("/")
    }
    //TODO: move this method
    case req @ GET(Path(Seg("state" :: project :: "worklog" :: Nil))) => CookieToken(req) match {
      case Some(rt) => JsonContent ~> Json(model.WorkLog.listByProject(rt.user, project).toList)
      case _ => Forbidden ~> Redirect("/")
    }
    case req @ GET(Path(Seg("state" :: "sync" :: Nil))) => CookieToken(req) match {
      case Some(rt) =>
        model.User.sync(rt)
        Ok ~> Redirect("/")
      case _ => Forbidden ~> Redirect("/")
    }
  }
}
