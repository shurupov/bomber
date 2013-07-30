package bomber.bombclient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:14
 */
public class BombChannelFutureListener implements ChannelFutureListener {

    private static final Logger logger = LoggerFactory.getLogger(BombChannelFutureListener.class);

    private List<Channel> channels;

    public BombChannelFutureListener(List<Channel> channels) {
        this.channels = channels;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        channels.add(future.channel());

        logger.info("Channel created");

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/");

        future.channel().writeAndFlush(request).sync();
        future.channel().read();

    }

}
