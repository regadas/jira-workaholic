package eu.regadas.jira

import eu.regadas._
import model._
import java.net.URL
import com.atlassian.jira.rpc.soap.client._

abstract class Client(val service: JiraSoapService) {
  def this(url: URL) = this((new JiraSoapServiceServiceLocator).getJirasoapserviceV2(url))
}

case class Api(url: URL) extends Client(url) {

  def login(user: String, password: String) = new ClientToken(service.login(user, password), user)

  def user(auth: ClientToken) = service.getUser(auth.token, auth.user)

  object project {

    def list(auth: ClientToken): List[Project] = service.getProjectsNoSchemes(auth.token).toList

    def worklogs(auth: ClientToken, project: String, max: Option[Int] = None): List[WorkLog] =
      issue.list(auth, project, max) flatMap { i => issue.worklogs(auth, i.key) }
  }

  object issue {
    def list(auth: ClientToken, project: String, max: Option[Int] = None): List[Issue] =
      service.getIssuesFromJqlSearch(auth.token, "project = \"%s\" AND assignee = \"%s\"" format (project, auth.user), max.getOrElse(20)).toList

    def worklogs(auth: ClientToken, issue: String): List[WorkLog] =
      (service.getWorklogs(auth.token, issue) filter { wl => wl.getAuthor == auth.user } toList) map { remote => WorkLog(issue, remote) }
  }
}
