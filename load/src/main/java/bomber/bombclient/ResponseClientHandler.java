package bomber.bombclient;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Eugene Shurupov
 * Date: 29.07.13
 * Time: 19:02
 */
public class ResponseClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ResponseClientHandler.class);

    private HttpResponse response;
    private String contentStr;

    public Object waiter;

    public long requestBeginTime;
    public boolean responseReceived;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {

            if (msg instanceof HttpResponse) {
                response = (HttpResponse) msg;
                if (!HttpResponseStatus.OK.equals(response.getStatus())) {
                    Bomber.instance().failed.incrementAndGet();
                }
            }

            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                if (content.content().capacity() > 0) {
                    contentStr = new String(content.content().array());
                }
                logger.debug("http content received {}", contentStr);
            }

            if (msg instanceof LastHttpContent) {

                if (HttpResponseStatus.OK.equals(response.getStatus())) {

                    if ("\r\n".equals(contentStr)) {
                        Bomber.instance().notFound0.incrementAndGet();
                        return;
                    }

                    if ("1\r\n".equals(contentStr)) {
                        Bomber.instance().notFound1.incrementAndGet();
                        return;
                    }

                    Bomber.instance().successful.incrementAndGet();

                    int responseTime = (int) (System.currentTimeMillis() - requestBeginTime);

                    Bomber.instance().responseTime.add(responseTime);

                    synchronized (waiter) {
                        responseReceived = true;
                        waiter.notify();
                    }

                }


            }
        } catch (Exception e) {
            logger.error("ResponseClientHandler", e);
            Bomber.instance().failed.incrementAndGet();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Bomber.instance().failed.incrementAndGet();
        logger.debug("Response is broken", cause);
        if (cause instanceof ReadTimeoutException) {
            Bomber.instance().timeout.incrementAndGet();
        }
        synchronized (waiter) {
            waiter.notify();
        }
    }
}
