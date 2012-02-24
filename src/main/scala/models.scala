package eu.regadas.model

import net.liftweb.json.JsonDSL._
import com.atlassian.jira.rpc.soap.client._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import eu.regadas.Db._
import java.util.Locale
import org.joda.time.Period
import com.mongodb.BasicDBList

object User {
  def hasState(user: String): Boolean = collection(WorkLog.COLL_NAME) { coll =>
    coll.count(DBObject("user" -> user)) > 0
  }
}

case class Project(key: String, name: String, url: String) {
  def save(user: String) = collection(Project.COLL_NAME) { coll =>
    coll += (Project.toDBObject(this) += ("user" -> user))
  }
}

object Project {
  val COLL_NAME = "projects"

  def findByUser(user: String) = collection(COLL_NAME) { coll =>
    coll.find(DBObject("user" -> user))
  }

  implicit def fromRemote(remote: RemoteProject) =
    Project(remote.getKey, remote.getName, remote.getUrl)

  implicit def projectToJson(project: Project) =
    ("name" -> project.name) ~ ("key" -> project.key)

  implicit def fromListRemote(remotes: List[RemoteProject]) =
    remotes map (fromRemote(_))

  implicit val toDBObject = (project: Project) =>
    DBObject("key" -> project.key, "name" -> project.name, "url" -> project.url)

  implicit val fromDBObject = (obj: DBObject) =>
    Project(obj.getAs[String]("key").get,
      obj.getAs[String]("name").get,
      obj.getAs[String]("url").get)
}

case class Issue(key: String, summary: String) {
  def save(user: String) = collection(Project.COLL_NAME) { coll =>
    coll += (Issue.toDBObject(this) += ("user" -> user))
  }
}

object Issue {
  val COLL_NAME = "issues"

  implicit def fromRemote(remote: RemoteIssue) =
    Issue(remote.getKey, remote.getSummary)

  implicit def issueToJson(issue: Issue) =
    ("summary" -> issue.summary) ~ ("key" -> issue.key)

  implicit def fromListRemote(remotes: List[RemoteIssue]) =
    remotes map (fromRemote(_))

  implicit val toDBObject = (issue: Issue) =>
    DBObject("summary" -> issue.summary, "key" -> issue.key)

  implicit val fromDBObject = (obj: DBObject) =>
    Issue(obj.getAs[String]("key").get, obj.getAs[String]("summary").get)
}

case class WorkLog(id: Option[String], project: String, issue: String, spentInSeconds: Long, start: DateTime, created: DateTime) {

  lazy val end = start.plus(spentInSeconds * 1000)

  def spent = {
    val period = new Period(this.spentInSeconds * 1000)
    "%sw %sd %sh %sm" format (period.getWeeks, period.getDays, period.getHours, period.getMinutes)
  }

  def save(user: String) = collection(Project.COLL_NAME) { coll =>
    coll += (WorkLog.toDBObject(this) += ("user" -> user))
  }
}

object WorkLog {
  val COLL_NAME = "worklogs"
  private val DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def apply(project: String, issue: String, remote: RemoteWorklog) =
    new WorkLog(Option(remote.getId),
      project: String,
      issue,
      remote.getTimeSpentInSeconds,
      new DateTime(remote.getStartDate),
      new DateTime(remote.getCreated))

  def remove(user: String, project: String, issue: String, created: DateTime) = collection(COLL_NAME) { coll =>
    coll.findAndRemove(DBObject("user" -> user,
      "project" -> project,
      "issue" -> issue,
      "created" -> created))
  }

  implicit val toRemoteWorklog = (wl: WorkLog) => {
    val locale = Locale.ENGLISH
    val log = new RemoteWorklog()
    log.setStartDate(wl.start.toCalendar(locale))
    log.setCreated(wl.created.toCalendar(locale))
    log.setTimeSpentInSeconds(wl.spentInSeconds)
    log.setTimeSpent(wl.spent)
    log
  }

  implicit val worklogToJson = (worklog: WorkLog) => ("id" -> worklog.id) ~
    ("title" -> worklog.issue) ~
    ("project" -> worklog.project) ~
    ("issue" -> worklog.issue) ~
    // FIXME: DateTime conversions
    ("start" -> DATE_FORMAT.print(worklog.start)) ~
    ("end" -> DATE_FORMAT.print(worklog.end)) ~
    ("created" -> worklog.created.toDate.getTime)

  implicit val fromDBObject = (obj: DBObject) =>
    WorkLog(obj.getAs[String]("id"),
      obj.getAs[String]("project").get,
      obj.getAs[String]("issue").get,
      obj.getAs[Long]("spentInSeconds").get,
      obj.getAs[DateTime]("start").get,
      obj.getAs[DateTime]("created").get)

  implicit val toDBObject = (wl: WorkLog) => DBObject("id" -> wl.id,
    "project" -> wl.project,
    "issue" -> wl.issue,
    "spentInSeconds" -> wl.spentInSeconds,
    "start" -> wl.start,
    "created" -> wl.created)
}
