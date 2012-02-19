package eu.regadas.jira

import eu.regadas._
import model._
import java.net.URL
import com.atlassian.jira.rpc.soap.client._
import com.weiglewilczek.slf4s.Logging

abstract class Client(val service: JiraSoapService) {
  def this(url: URL) = this((new JiraSoapServiceServiceLocator).getJirasoapserviceV2(url))
}

case class Api(url: URL) extends Client(url) with Logging {

  def login(user: String, password: String) = Clock("jira auth", logger) {
    new ClientToken(service.login(user, password), user)
  }

  def user(auth: ClientToken) = Clock("jira user info", logger) {
    service.getUser(auth.token, auth.user)
  }

  object project {
    def list(auth: ClientToken): List[Project] = Clock("jira project list", logger) {
      service.getProjectsNoSchemes(auth.token).toList
    }

    def worklogs(auth: ClientToken, project: String, max: Option[Int] = None): List[WorkLog] = Clock("jira projects worklogs", logger) {
      issue.list(auth, project, max) flatMap { i => issue.worklogs(auth, project, i.key) }
    }
  }

  object issue {
    def list(auth: ClientToken, project: String, max: Option[Int] = None): List[Issue] = Clock("jira issue list", logger){
      service.getIssuesFromJqlSearch(auth.token, "project = \"%s\" AND assignee = \"%s\"" format (project, auth.user), max.getOrElse(20)).toList
    }

    def worklogs(auth: ClientToken, project: String, issue: String): List[WorkLog] = Clock("jira issue worklog", logger){
      (service.getWorklogs(auth.token, issue) filter { wl => wl.getAuthor == auth.user } toList) map { remote => WorkLog(project, issue, remote) }
    }
  }
}
