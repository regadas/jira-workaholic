package eu.regadas

import unfiltered.netty.ExceptionHandler
import com.atlassian.jira.rpc.soap.client.RemoteAuthenticationException
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.ChannelBuffers
import com.weiglewilczek.slf4s.Logging

trait JiraWorkAholicErrorResponse extends Logging { self: ExceptionHandler =>
  def onException(ctx: ChannelHandlerContext, t: Throwable) {
    val ch = ctx.getChannel
    if (ch.isOpen) try {
      t match {
        case e: RemoteAuthenticationException =>
          logger.warn("authentication exception ... jira session timeout")
          val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN)
          ch.write(res).addListener(ChannelFutureListener.CLOSE)
        case unknown =>
          logger.error("Exception caught handling request: %s" format unknown.getMessage, unknown)
          val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR)
          res.setContent(ChannelBuffers.copiedBuffer("Internal Server Error".getBytes("utf-8")))
          ch.write(res).addListener(ChannelFutureListener.CLOSE)
      }
    } catch {
      case _ => ch.close()
    }
  }
}
