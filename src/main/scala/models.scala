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

case class WorkLog(title: String, spentInSeconds: Long, start: DateTime) {

  lazy val end = start.plus(spentInSeconds * 1000)

  private def toDBObject(user: String, projectKey: String, issueKey: String) = DBObject(
    "user" -> user,
    "project" -> projectKey,
    "issue" -> issueKey,
    "title" -> this.title,
    "spentInSeconds" -> this.spentInSeconds,
    "start" -> this.start)

  def cache(user: String, projectKey: String, issueKey: String) = collection("worklogs") { coll =>
    coll += this.toDBObject(user, projectKey, issueKey)
  }
}

object WorkLog {

  private val DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def apply(issueKey: String, remote: RemoteWorklog) =
    new WorkLog(issueKey, remote.getTimeSpentInSeconds, new DateTime(remote.getStartDate))

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

  implicit def worklogToJson(worklog: WorkLog) = ("title" -> worklog.title) ~
    ("start" -> DATE_FORMAT.print(worklog.start)) ~
    ("end" -> DATE_FORMAT.print(worklog.end)) ~
    ("allDay" -> false)

  implicit val dbObjectToWorkLog = (obj: DBObject) =>
    WorkLog(obj.getAs[String]("title").get, obj.getAs[Long]("spentInSeconds").get, obj.getAs[DateTime]("start").get)
}
