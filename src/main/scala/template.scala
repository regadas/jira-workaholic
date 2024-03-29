package eu.regadas

import unfiltered.request._
import unfiltered.response._

trait Template {

  case class Layout(docType: xml.dtd.DocType, nodes: scala.xml.Elem) extends ComposeResponse(
    HtmlContent ~> {
      val data = new java.io.StringWriter()
      xml.XML.write(data, nodes, "UTF-8", xmlDecl = false, doctype = docType)
      ResponseString(data.toString)
    })

  def layout(head: scala.xml.NodeSeq)(js: scala.xml.NodeSeq)(navbar: scala.xml.NodeSeq)(body: scala.xml.NodeSeq) = {
    Layout(xml.dtd.DocType("html", xml.dtd.SystemID("about:legacy-compat"), Nil),
      <html>
        <head>
          <meta charset="utf-8"/>
          <title>Jira WorkLog</title>
          <meta name="description" content=""/>
          <meta name="author" content=""/>
          <link rel="stylesheet" type="text/css" href="/css/bootstrap.min.css"/>
          <link rel="stylesheet" type="text/css" href="/css/fullcalendar.css"/>
          <link rel="stylesheet" type="text/css" href="/css/jquery.timepicker.css"/>
          <link rel="stylesheet" type="text/css" href="/css/app.css"/>
          { head }
        </head>
        <body>
          <div class="navbar navbar-fixed-top">
            <div class="navbar-inner">
              <div class="container-fluid">
                <a data-target=".nav-collapse" data-toggle="collapse" class="btn btn-navbar">
                  <span class="i-bar"></span>
                  <span class="i-bar"></span>
                  <span class="i-bar"></span>
                </a>
                <a href="#" class="brand">Jira WorkLog</a>
                <div class="nav-collapse">
                  { navbar }
                </div><!--/.nav-collapse -->
              </div>
            </div>
          </div>
          <div class="container">
            <div class="row">
              { body }
            </div><!--/row-->
            <hr/>
            <footer>
              <p>&copy; Company 2012</p>
            </footer>
          </div>
          <script type="text/javascript" src="/js/jquery.js"></script>
          <script type="text/javascript" src="/js/jquery-ui.js"></script>
          <script type="text/javascript" src="/js/jquery.timepicker.min.js"></script>
          <script type="text/javascript" src="/js/spin.min.js"></script>
          <script type="text/javascript" src="/js/jquery.spin.js"></script>
          <script type="text/javascript" src="/js/hogan.js"></script>
          <script type="text/javascript" src="/js/fullcalendar.min.js"></script>
          <script type="text/javascript" src="/js/bootstrap.min.js"></script>
          <script type="text/javascript" src="/js/bootstrap-modal.js"></script>
          <script type="text/javascript" src="/js/bootstrap-transition.js"></script>
          <script type="text/javascript" src="/js/bootstrap-tooltip.js"></script>
          { js }
        </body>
      </html>)
  }
  def fluidLayout(head: scala.xml.NodeSeq)(js: scala.xml.NodeSeq)(navbar: scala.xml.NodeSeq)(sidebar: scala.xml.NodeSeq)(body: scala.xml.NodeSeq) = {
    Layout(xml.dtd.DocType("html", xml.dtd.SystemID("about:legacy-compat"), Nil),
      <html>
        <head>
          <meta charset="utf-8"/>
          <title>Jira WorkLog</title>
          <meta name="description" content=""/>
          <meta name="author" content=""/>
          <link rel="stylesheet" type="text/css" href="/css/bootstrap.min.css"/>
          <link rel="stylesheet" type="text/css" href="/css/fullcalendar.css"/>
          <link rel="stylesheet" type="text/css" href="/css/jquery.timepicker.css"/>
          <link rel="stylesheet" type="text/css" href="/css/app.css"/>
          { head }
        </head>
        <body>
          <div class="navbar navbar-fixed-top">
            <div class="navbar-inner">
              <div class="container-fluid">
                <a data-target=".nav-collapse" data-toggle="collapse" class="btn btn-navbar">
                  <span class="i-bar"></span>
                  <span class="i-bar"></span>
                  <span class="i-bar"></span>
                </a>
                <a href="#" class="brand">Jira WorkLog</a>
                <div class="nav-collapse">
                  { navbar }
                </div><!--/.nav-collapse -->
              </div>
            </div>
          </div>
          <div class="container-fluid">
            <div class="row-fluid">
              { sidebar }
              <div class="span9">
                { body }
              </div><!--/span-->
            </div><!--/row-->
            <hr/>
            <footer>
              <p>&copy; Company 2012</p>
            </footer>
          </div>
          <script type="text/javascript" src="/js/jquery.js"></script>
          <script type="text/javascript" src="/js/jquery-ui.js"></script>
          <script type="text/javascript" src="/js/jquery.timepicker.min.js"></script>
          <script type="text/javascript" src="/js/spin.min.js"></script>
          <script type="text/javascript" src="/js/jquery.spin.js"></script>
          <script type="text/javascript" src="/js/hogan.js"></script>
          <script type="text/javascript" src="/js/fullcalendar.min.js"></script>
          <script type="text/javascript" src="/js/bootstrap.min.js"></script>
          <script type="text/javascript" src="/js/bootstrap-modal.js"></script>
          <script type="text/javascript" src="/js/bootstrap-transition.js"></script>
          <script type="text/javascript" src="/js/bootstrap-tooltip.js"></script>
          <script type="text/javascript" src="/js/app.js"></script>
          { js }
        </body>
      </html>)
  }

  def home(navbar: scala.xml.NodeSeq)(sidebar: scala.xml.NodeSeq)(body: scala.xml.NodeSeq) = fluidLayout(Nil)(Nil)(navbar)(
    <div class="span3">
      <div class="well sidebar-nav">
        <ul class="nav nav-list">
          { sidebar }
        </ul>
      </div>
    </div>)(body)

  def login = layout(Nil)(Nil)(Nil)(
    <div class="span5">
      <h1>Login</h1>
      <div class="row">
        <div class="span5">
          <form class="well" action="/login" method="POST">
            <label>Your Jira Usename</label>
            <input type="text" class="spa3" name="user"/>
            <label>Password</label>
            <input type="password" class="span3" name="password"/>
            <button class="btn" type="submit">Login</button>
          </form>
        </div>
      </div>
    </div>)

}
