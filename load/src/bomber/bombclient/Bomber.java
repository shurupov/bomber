package bomber.bombclient;

import bomber.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:18
 */
public class Bomber implements Runnable {

    private static final int CHANNEL_COUNT = 10;
    private static final String HOST = "localhost";
    public static final int PORT = 5555;

    private List<Channel> channels;

    private static final Logger logger = LoggerFactory.getLogger(Bomber.class);

    @Override
    public void run() {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Config.instance();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ClientChannelInitializer());

            createChannels(b, CHANNEL_COUNT);

        } catch (InterruptedException e) {
            logger.error("Bomber Interrupted Exception", e);
        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    private void createChannels(Bootstrap b, int count) throws InterruptedException {

        channels = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Start the client.
            ChannelFuture channelFuture = b.connect(HOST, PORT).sync()
                    .addListener(new BombChannelFutureListener(channels));

            // Wait until the connection is closed.
            channelFuture.channel().closeFuture().sync();
        }
    }
}
