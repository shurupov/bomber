package bomber.bombclient;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

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

        pipeline.addLast("decoder", new HttpResponseDecoder());
        pipeline.addLast(new ResponseClientHandler());

    }

}
