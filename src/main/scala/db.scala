package eu.regadas

import com.mongodb.casbah._
import com.mongodb.MongoURI
import java.net.URI
import com.mongodb.casbah.commons.conversions.scala._

object Db {
  
  lazy val connection = {
    val uri = new URI(Props.get("MONGO_URI"))
    try {
      RegisterJodaTimeConversionHelpers()
      val conn = MongoConnection(uri.getHost, uri.getPort)
      val name = uri.getPath.drop(1)
      val mongo = conn(name)
      val Array(user, pass) = uri.getUserInfo.split(":")
      mongo.authenticate(user, pass)
      mongo
    } catch {
      case e: java.io.IOException =>
        sys.error("Error occured whilst connecting to mongo (%s): %s" format (uri, e.getMessage))
        throw e
    }
  }

  def collection[T](name: String)(f: MongoCollection => T): T = f(connection(name))
}
