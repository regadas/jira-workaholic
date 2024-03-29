package eu.regadas.jira

import eu.regadas._
import model._
import java.net.URL
import com.atlassian.jira.rpc.soap.client._
import com.weiglewilczek.slf4s.{ Logger, Logging }

abstract class Client(val service: JiraSoapService) {
  def this(url: URL) = this((new JiraSoapServiceServiceLocator).getJirasoapserviceV2(url))
}

case class Api(url: URL) extends Client(url) with Logging {

  implicit val log: Logger = logger

  def login(user: String, password: String) = Clock("jira auth") {
    new ClientToken(service.login(user, password), user)
  }

  def logout(auth: ClientToken) = Clock("jira logout") {
    service.logout(auth.user)
  }

  def user(auth: ClientToken) = Clock("jira user info") {
    service.getUser(auth.token, auth.user)
  }

  object project {
    def list(auth: ClientToken): List[Project] = Clock("jira project list") {
      service.getProjectsNoSchemes(auth.token).toList
    }

    def worklogs(auth: ClientToken, project: String, max: Option[Int] = None): List[WorkLog] = Clock("jira projects worklogs") {
      issue.list(auth, project, max) flatMap { i => issue.worklogs(auth, i.key) }
    }
  }

  object issue {
    def search(auth: ClientToken, term: String, max: Int = 10): List[Issue] = Clock("search issues with term %s" format term) {
      service.getIssuesFromTextSearchWithLimit(auth.token, term, 0, max).toList
    }

    def find(auth: ClientToken, key:String): Issue = Clock("find by key") {
      service.getIssue(auth.token, key)
    }

    def list(auth: ClientToken, project: String, max: Option[Int] = None): List[Issue] = Clock("jira issue list") {
      service.getIssuesFromJqlSearch(auth.token, "project = \"%s\" AND assignee = \"%s\"" format (project, auth.user), max.getOrElse(20)).toList
    }

    def worklogs(auth: ClientToken, issue: String): List[WorkLog] = Clock("jira issue worklog") {
      val remoteIssue = service.getIssue(auth.token, issue)
      (service.getWorklogs(auth.token, issue) filter { wl => wl.getAuthor == auth.user } toList) map { remote => WorkLog(remoteIssue.getProject, issue, remote) }
    }
  }

  object worklog {
    def add(auth: ClientToken, issue: String, worklog: WorkLog) = Clock("jira add worklog") {
      service.addWorklogAndAutoAdjustRemainingEstimate(auth.token, issue, worklog)
    }
  }
}
