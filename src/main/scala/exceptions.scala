package eu.regadas

import unfiltered.netty.ExceptionHandler
import com.atlassian.jira.rpc.soap.client.RemoteAuthenticationException
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.ChannelBuffers

trait JiraWorkAholicErrorResponse { self: ExceptionHandler =>
  def onException(ctx: ChannelHandlerContext, t: Throwable) {
    val ch = ctx.getChannel
    if (ch.isOpen) try {
      t match {
        case e: RemoteAuthenticationException =>
          val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED)
          res.setStatus(HttpResponseStatus.FOUND)
          res.setHeader(HttpHeaders.Names.LOCATION, "/logout")
          ch.write(res).addListener(ChannelFutureListener.CLOSE)
        case unknown =>
          System.err.println("Exception caught handling request: %s" format unknown.getMessage)
          val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR)
          res.setContent(ChannelBuffers.copiedBuffer("Internal Server Error".getBytes("utf-8")))
          ch.write(res).addListener(ChannelFutureListener.CLOSE)
      }
    } catch {
      case _ => ch.close()
    }
  }
}