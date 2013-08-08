package bomber.bombclient;

import bomber.config.Config;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:03
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("encoder", new HttpRequestEncoder());


        pipeline.addLast("timeoutHandler", new ReadTimeoutHandler(Config.instance().timeout, TimeUnit.MILLISECONDS));
        pipeline.addLast("decoder", new HttpResponseDecoder());
        pipeline.addLast("handler", new ResponseClientHandler());

    }

}
