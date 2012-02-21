package eu.regadas

import unfiltered._
import unfiltered.netty._
import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie
import jira._
import java.net.URL
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import com.weiglewilczek.slf4s._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object Browser {
  /** Paths for which we care not */
  def trapdoor: Cycle.Intent[Any, Any] = {
    case GET(Path("/favicon.ico")) => NotFound
  }
}