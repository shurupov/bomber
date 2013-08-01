package bomber.bombclient;

import bomber.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    public Map<Long, Channel> channels = new ConcurrentHashMap<>();
    public List<Integer> responseTime = new CopyOnWriteArrayList<>();

    public AtomicLong all = new AtomicLong(0);
    public AtomicLong successful = new AtomicLong(0);
    public AtomicLong notFound1 = new AtomicLong(0);
    public AtomicLong notFound0 = new AtomicLong(0);
    public AtomicLong failed = new AtomicLong(0);

    @Override
    public void run() {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Config.instance();

        try {

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.handler(new ClientChannelInitializer());

            new Thread(new ThreadCreator(channels, bootstrap)).start();

            do {
                Thread.sleep(1000);
                log();
            } while (all.get() < Config.instance().bombsCount);

            end();

        } catch (InterruptedException e) {
            logger.error("Bomber Interrupted Exception", e);
        } finally {
            workerGroup.shutdownGracefully();
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

    private void log() {

        logger.info("all: {}, successful: {}, notFound0: {}, notFound1: {}, failed: {}",
                all, successful, notFound0, notFound1, failed);
        List<Integer> tmpResponseTimes = new ArrayList<>(responseTime);

        Collections.sort(tmpResponseTimes);
        if (tmpResponseTimes.size() > 0) {
            int step = tmpResponseTimes.size() / 10;
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 10; i++) {
                sb.append(i * 10);
                sb.append('%');
                sb.append(':');
                sb.append(' ');
                sb.append(tmpResponseTimes.get( step * i - 1 ));
                sb.append(' ');
            }
            logger.info(sb.toString());
        }

        logger.info("Threads: {}", channels.size());

        logger.info("\n");
    }

    private void end() throws InterruptedException {
        for (Channel channel : channels.values()) {
            channel.close().sync();
        }
    }
}
