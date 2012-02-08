package eu.regadas.jira

import eu.regadas.model._
import java.net.URL
import com.atlassian.jira.rpc.soap.client._

object Session {
  private val url = sys.env.get("JIRA_WS") match {
    case Some(url) => new java.net.URL(url)
    case _ => throw new Exception("JIRA_WS env variable was not set")
  }
  lazy val service = (new JiraSoapServiceServiceLocator).getJirasoapserviceV2(url)
}

object Client {

  def login(user: String, password: String) = Session.service.login(user, password)
  def user(token: String, username: String) = Session.service.getUser(token, username)

  object Issue {
    def list(token: String, username: String, project: String, max: Option[Int] = None): List[Issue] =
      Session.service.getIssuesFromJqlSearch(token, "project = \"%s\" AND assignee = \"%s\"" format (project, username), max.getOrElse(20)).toList

    def worklogs(token: String, username: String, issue: String): List[WorkLog] =
      (Session.service.getWorklogs(token, issue) filter { wl => wl.getAuthor == username } toList) map { remote => WorkLog(issue, remote) }
  }

  object Project {

    def list(token: String): List[Project] = Session.service.getProjectsNoSchemes(token)

    def worklogs(token: String, username: String, project: String, max: Option[Int] = None): List[WorkLog] =
      Issue.list(token, username, project, max) flatMap { issue =>
        Issue.worklogs(token, username, issue.key)
      }
  }

}