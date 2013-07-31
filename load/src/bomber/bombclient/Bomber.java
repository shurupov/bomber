package bomber.bombclient;

import bomber.config.Config;
import bomber.config.FullUri;
import bomber.config.Param;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:18
 */
public class Bomber implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Bomber.class);

    private static Bomber instance;

    public List<Channel> channels;
    private final Random r = new Random();

    public AtomicLong all = new AtomicLong(0);
    public AtomicLong successful = new AtomicLong(0);
    public AtomicLong notFound1 = new AtomicLong(0);
    public AtomicLong notFound0 = new AtomicLong(0);
    public AtomicLong failed = new AtomicLong(0);

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
                b.option(ChannelOption.TCP_NODELAY, true);
                b.handler(new ClientChannelInitializer());

                createChannels(b);

                do {
                    Thread.sleep(100);
                    logger.info("all: {}, successful: {}, notFound0: {}, notFound1: {}, failed: {}",
                            all, successful, notFound0, notFound1, failed);
                } while (all.get() < Config.instance().bombsCount);

            }

        } catch (InterruptedException e) {
            logger.error("Bomber Interrupted Exception", e);
        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    private void createChannels(Bootstrap b) throws InterruptedException {

        channels = new CopyOnWriteArrayList<>();
        for (int i = 0; i < Config.instance().channelCount; i++) {

            new Thread(new ChannelRunnable(b)).start();

        }
    }

    public String getRandUri() {
        List<FullUri> fullUris = Config.instance().fullUris.fullUris;
        final FullUri uri;
        if (fullUris.size() == 1) {
            uri = fullUris.get(0);
        } else {
            uri = fullUris.get((int) Math.floor(r.nextFloat() * fullUris.size()));
        }
        List<Param> params = uri.params.params;
        final Param param;
        if (params.size() == 1) {
            param = params.get(0);
        } else {
            param = params.get((int) Math.floor(r.nextFloat() * params.size()));
        }
        List<String> values = param.values;
        final String paramValue = values.get((int) Math.floor(r.nextFloat() * values.size()));

        StringBuilder sb = new StringBuilder(uri.path);
        sb.append('?');
        sb.append(param.name);
        sb.append('=');
        sb.append(paramValue);

        logger.info("uri = {}", sb.toString());

        return sb.toString();
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
