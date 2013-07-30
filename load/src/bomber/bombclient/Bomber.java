package bomber.bombclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:18
 */
public class Bomber implements Runnable {

    private static final int CHANNEL_COUNT = 10;
    private static final String HOST = "localhost";
    public static final int PORT = 5555;

    private Channel[] channels;

    private static final Logger logger = LoggerFactory.getLogger(Bomber.class);

    @Override
    public void run() {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ClientChannelInitializer());

            channels = new Channel[CHANNEL_COUNT];

            for (int i = 0; i < CHANNEL_COUNT; i++) {
                // Start the client.
                ChannelFuture channelFuture = b.connect(HOST, PORT).sync()
                        .addListener(new BombChannelFutureListener());

                // Wait until the connection is closed.
                channelFuture.channel().closeFuture().sync();
            }

        } catch (InterruptedException e) {
            logger.error("Bomber Interrupted Exception", e);
        } finally {
            workerGroup.shutdownGracefully();
        }

    }
}
