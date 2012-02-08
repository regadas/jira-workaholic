package eu.regadas

import unfiltered._
import org.jboss.netty.handler.codec.http.HttpChunkAggregator

object Server {
  import unfiltered.response._
  def main(a: Array[String]):Unit = {
    netty.Http(sys.props.getOrElse("PORT", 5000).asInstanceOf[Int])
      .resources(getClass().getResource("/www/"))
      .handler(new HttpChunkAggregator(65536))
      .handler(netty.cycle.Planify {
        Browser.trapdoor
      })
      .handler(netty.cycle.Planify {
         Browser.authentication orElse Browser.home orElse Browser.projects orElse Browser.issues
      })
      .run(s => a match {
       case a@Array(_*) if(a contains "-b") => util.Browser.open(s.url)
       case _ => ()
      })
  }
}