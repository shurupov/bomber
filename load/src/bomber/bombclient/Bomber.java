package bomber.bombclient;

import bomber.config.Config;
import bomber.config.FullUri;
import bomber.config.Param;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:18
 */
public class Bomber implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Bomber.class);

    private static Bomber instance;

    private List<Channel> channels;
    private final Random r = new Random();

    private Bomber() {
        r.setSeed(System.currentTimeMillis());
    }

    @Override
    public void run() {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Config.instance();

        try {
            synchronized (this) {

                Bootstrap b = new Bootstrap();
                b.group(workerGroup);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.handler(new ClientChannelInitializer());

                createChannels(b, Config.instance().channelCount);

                wait();

                bomb();
            }

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
            ChannelFuture channelFuture = b.connect(Config.instance().host, Config.instance().port).sync()
                    .addListener(new BombChannelFutureListener(channels));

            // Wait until the connection is closed.
            channelFuture.channel().closeFuture();
        }
    }

    private String getRandUri() {
        List<FullUri> fullUris = Config.instance().fullUris.fullUris;
        final FullUri uri;
        if (fullUris.size() == 1) {
            uri = fullUris.get(0);
        } else {
            uri = fullUris.get((int) (r.nextFloat() * fullUris.size()));
        }
        List<Param> params = uri.params.params;
        final Param param;
        if (params.size() == 1) {
            param = params.get(0);
        } else {
            param = params.get((int) (r.nextFloat() * params.size()));
        }
        List<String> values = param.values;
        final String paramValue = values.get((int) r.nextFloat() * params.size());

        StringBuilder sb = new StringBuilder(uri.path);
        sb.append('?');
        sb.append(param.name);
        sb.append('=');
        sb.append(paramValue);

        logger.info("uri = {}", sb.toString());

        return sb.toString();
    }

    synchronized public void bomb() throws InterruptedException {

        logger.info("bomb");

        for (Channel channel : channels) {
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                    getRandUri());

            channel.writeAndFlush(request).sync();
            channel.read();
        }

    }

    public static Bomber instance() {
        if (instance == null) {
            synchronized (Bomber.class) {
                if (instance == null) {
                    instance = new Bomber();
                }
            }
        }
        return instance;
    }
}
