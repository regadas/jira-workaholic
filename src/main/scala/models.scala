package eu.regadas.model

import net.liftweb.json.JsonDSL._
import com.atlassian.jira.rpc.soap.client._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import com.mongodb.casbah.Imports._
import eu.regadas.Db._

case class Project(key: String, name: String, url: String)

object Project {
  implicit def fromRemote(remote: RemoteProject) =
    new Project(remote.getKey, remote.getName, remote.getUrl)

  implicit def projectToJson(project: Project) =
    ("name" -> project.name) ~ ("key" -> project.key)

  implicit def fromListRemote(remotes: List[RemoteProject]) =
    remotes map (fromRemote(_))
}

case class Issue(key: String, summary: String)

object Issue {
  implicit def fromRemote(remote: RemoteIssue) =
    new Issue(remote.getKey, remote.getSummary)

  implicit def issueToJson(issue: Issue) =
    ("summary" -> issue.summary) ~ ("key" -> issue.key)

  implicit def fromListRemote(remotes: List[RemoteIssue]) =
    remotes map (fromRemote(_))
}

case class WorkLog(id: Option[String], project: String, issue: String, spentInSeconds: Long, start: DateTime, created: DateTime) {

  lazy val end = start.plus(spentInSeconds * 1000)

  private def toDBObject(user: String) = DBObject("id" -> this.id,
    "user" -> user,
    "project" -> this.project,
    "issue" -> this.issue,
    "spentInSeconds" -> this.spentInSeconds,
    "start" -> this.start,
    "created" -> this.created)

  def cache(user: String) = collection("worklogs") { coll =>
    coll += this.toDBObject(user)
  }
}

object WorkLog {

  private val DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def apply(project: String, issue: String, remote: RemoteWorklog) =
    new WorkLog(Option(remote.getId),
      project: String,
      issue,
      remote.getTimeSpentInSeconds,
      new DateTime(remote.getStartDate),
      new DateTime(remote.getCreated))

  def cached(user: String): Boolean = collection("worklogs") { coll =>
    coll.find(DBObject("user" -> user)).size > 0
  }

  def cachedByUser(user: String): Iterator[WorkLog] = collection("worklogs") { coll =>
    for { x <- coll.find(DBObject("user" -> user)) } yield x
  }

  def cachedByIssue(user: String, issueKey: String): Iterator[WorkLog] = collection("worklogs") { coll =>
    for { x <- coll.find(DBObject("user" -> user, "issue" -> issueKey)) } yield x
  }

  def cachedByProject(user: String, projectKey: String): Iterator[WorkLog] = collection("worklogs") { coll =>
    for { x <- coll.find(DBObject("user" -> user, "project" -> projectKey)) } yield x
  }

  def evict(user: String, project: String, issue: String, created: DateTime) = collection("worklogs") { coll =>
    coll.findOne(DBObject("user" -> user,
      "project" -> project,
      "issue" -> issue,
      "created" -> created)) map { wl => coll -= wl }
  }

  implicit def worklogToJson(worklog: WorkLog) = ("id" -> worklog.id) ~
    ("title" -> worklog.issue) ~
    ("project" -> worklog.project) ~
    ("issue" -> worklog.issue) ~
    // FIXME: DateTime conversions
    ("start" -> DATE_FORMAT.print(worklog.start)) ~
    ("end" -> DATE_FORMAT.print(worklog.end)) ~
    ("created" -> worklog.created.toDate.getTime)

  implicit val dbObjectToWorkLog = (obj: DBObject) =>
    WorkLog(obj.getAs[String]("id"),
      obj.getAs[String]("project").get,
      obj.getAs[String]("issue").get,
      obj.getAs[Long]("spentInSeconds").get,
      obj.getAs[DateTime]("start").get,
      obj.getAs[DateTime]("created").get)
}
