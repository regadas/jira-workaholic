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

object Site extends cycle.Plan 
  with cycle.SynchronousExecution 
  with JiraWorkAholicErrorResponse
  with Authentication
  with Template 
  with Logging {

  import QParams._

  implicit lazy val api = Api(new URL(Props.get("JIRA_WS")))

  def intent = (home /: Seq(projects, issues, user, search))(_ orElse _)

  def home: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/") & Cookies(c)) => auth(req) { rt =>
      home(
        <p class="navbar-text pull-right">Logged in as <a href="#">{ rt.user }</a> <a href="/logout">Logout</a></p>)(
          <li class="nav-header">Favorite Issues</li>
          <ul id="user-issues" class="nav nav-list"><li class="empty"><p>Drop here some issues</p></li></ul>
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
    } match {
      case Forbidden => Redirect("/login")
      case other => other 
    }
  }

import com.atlassian.jira.rpc.soap.client.RemoteAuthenticationException
  def search: Cycle.Intent[Any, Any] = {
    //FIXME: some problem in unfiltered with param extractor ....
    case req @ POST(Path("/search/issues") & Params(params)) => auth(req) { rt =>
      val expected = for {
        query <- lookup("query") is
          nonempty("query is empty") is
          required("missing query")
      } yield JsonContent ~> Json(api.issue.search(rt, query.get))
      expected(params) orFail { fails => BadRequest }
    }
  }

  def projects: Cycle.Intent[Any, Any] = {
    case req @ GET(Path("/projects")) => auth(req) { rt =>
        JsonContent ~> Json(api.project.list(rt))
    }
    case req @ GET(Path(Seg("projects" :: project :: "issues" :: Nil)) & Params(params)) => auth(req) { rt =>
      val expected = for {
        max <- lookup("max") is
          int { _ + " is not an integer" }
      } yield JsonContent ~> Json(api.issue.list(rt, project, max))
      expected(params) orFail { fails => BadRequest }
    }
    case req @ GET(Path(Seg("projects" :: project :: "worklogs" :: Nil)) & Params(params)) => auth(req) { rt =>
      val expected = for {
        max <- lookup("max") is
          int { _ + " is not an integer" }
      } yield JsonContent ~> Json(api.project.worklogs(rt, project, max))
      expected(params) orFail { fails => BadRequest }
    }
  }

  def issues: Cycle.Intent[Any, Any] = {
    case req @ Path(Seg("issues" :: key :: "worklogs" :: Nil)) => auth(req) { rt =>
      req match {
        case GET(_) => JsonContent ~> Json(api.issue.worklogs(rt, key))
        case POST(_) =>
          val json = parse(Body.string(req))
          for {
            JField("created", JInt(createdTime)) <- json
            JField("start", JInt(startTime)) <- json
            JField("end", JInt(endTime)) <- json
          } yield {
            val start = new DateTime(startTime.toLong)
            val created = new DateTime(createdTime.toLong)
            model.WorkLog(None, key, (endTime - startTime).toLong / 1000, start, created).save(rt.user)
          }
          Ok
      }
    }
    case req @ POST(Path(Seg("issues" :: issue :: "worklog" :: "delete" :: Nil))) => auth(req) { rt =>
      val json = parse(Body.string(req))
      for {
        JField("created", JInt(createdTime)) <- json
      } yield model.WorkLog.remove(rt.user, issue, new DateTime(createdTime.toLong))
      JsonContent ~> Ok
    }
  }

  def user: Cycle.Intent[Any, Any] = {
    case req @ Path("/user/issues") => auth(req) { rt =>
      req match {
        case GET(_) => JsonContent ~> Json(model.Issue.list(rt.user).toList)
        case POST(_) =>
          val json = parse(Body.string(req))
          for {
            JField("key", JString(key)) <- json
          } yield {
            api.issue.find(rt, key).save(rt.user)
          }
          Ok
        case _ => NotFound
      }
    }
    case req @ GET(Path(Seg("user" :: "state" :: Nil))) => auth(req) { rt =>
      val worklogs = model.WorkLog.listByUser(rt.user).toList
      JsonContent ~> Json(("state" -> (worklogs.size > 0)) ~ ("worklogs" -> worklogs))
    }
    case req @ GET(Path(Seg("user" :: "state" :: "sync" :: Nil))) => auth(req) { rt =>
      model.User.sync(rt)
      Ok ~> Redirect("/")
    }
  }
}
