package eu.regadas.model

import net.liftweb.json.JsonDSL._
import com.atlassian.jira.rpc.soap.client._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

case class Project(key: String, name: String, url: String)

object Project {

  implicit def fromRemote(remote: RemoteProject) = new Project(remote.getKey, remote.getName, remote.getUrl)

  implicit def projectToJson(project: Project) = ("name" -> project.name) ~ ("key" -> project.key)

  implicit def fromListRemote(remotes: Array[RemoteProject]) = remotes.toList map { project =>
    val p: Project = project
    p
  }

}

case class Issue(key: String, summary: String) {
  def url =  "https://servicos.multicert.com/jira/browse/%s" format key
}

object Issue {

  implicit def fromRemote(remote: RemoteIssue) = new Issue(remote.getKey, remote.getSummary)

  implicit def issueToJson(issue: Issue) = ("summary" -> issue.summary) ~ 
  ("key" -> issue.key) ~ ("url" -> issue.url)

  implicit def fromListRemote(remotes: List[RemoteIssue]) = remotes.toList map { issue =>
    val i: Issue = issue
    i
  }

}

case class WorkLog(title: String, spent: String, spentInSeconds: Long, start: DateTime) {
  def end = start.plus(spentInSeconds * 1000)
  
  def url =  "https://servicos.multicert.com/jira/browse/%s" format title
}

object WorkLog {

  private val DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
  
  def apply(issueKey: String, remote: RemoteWorklog) =
    new WorkLog(issueKey, remote.getTimeSpent, remote.getTimeSpentInSeconds, new DateTime(remote.getStartDate))

  implicit def fromRemote(remote: RemoteWorklog) =
    new WorkLog(remote.getId, remote.getTimeSpent, remote.getTimeSpentInSeconds, new DateTime(remote.getStartDate))

  implicit def worklogToJson(worklog: WorkLog) = {
    ("title" -> worklog.title) ~ ("spent" -> worklog.spent) ~
      ("start" -> DATE_FORMAT.print(worklog.start)) ~
      ("end" -> DATE_FORMAT.print(worklog.end)) ~ ("allDay" -> false) ~
      ("url" -> worklog.url)
  }

  implicit def fromListRemote(remotes: List[RemoteWorklog]) = remotes.toList map { worklog =>
    val wl: WorkLog = worklog
    wl
  }
}
