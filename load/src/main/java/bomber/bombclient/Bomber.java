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
import java.util.concurrent.*;
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

    private Bootstrap bootstrap;

    public AtomicLong all = new AtomicLong(0);
    public AtomicLong successful = new AtomicLong(0);
    public AtomicLong notFound1 = new AtomicLong(0);
    public AtomicLong notFound0 = new AtomicLong(0);
    public AtomicLong failed = new AtomicLong(0);
    public AtomicLong timeout = new AtomicLong(0);

    private long beginTime = 0;

    private List<ScheduledExecutorService> executorServices;

    @Override
    public void run() {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Config.instance();

        try {

            bootstrap = new Bootstrap();
            bootstrap.group(workerGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.handler(new ClientChannelInitializer());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        executeThreads();
                    } catch (InterruptedException e) {
                        logger.info("error creating threads");
                    }
                }
            }).start();

            beginTime = System.currentTimeMillis();

            collectInfoAndLog();

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

    private void executeThreads() throws InterruptedException {

        executorServices = new ArrayList<>(Config.instance().threadCount);
        for (int i = 0; i < Config.instance().threadCount; i++) {
            Thread.sleep(Config.instance().threadsIncreaseDelay);

            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleWithFixedDelay(new ChannelRunnable(bootstrap),
                    0, Config.instance().threadRelaxTime, TimeUnit.MILLISECONDS);

            executorServices.add(service);
        }

    }

    private void collectInfoAndLog() throws InterruptedException {

        do {
            Thread.sleep(1000);

            logger.info("all: {}, successful: {}, notFound0: {}, notFound1: {}, failed: {}, timeout {}",
                    all.get(), successful, notFound0, notFound1, failed, timeout);
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
                    try {
                        sb.append(tmpResponseTimes.get( step * i - 1 ));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        sb.append(tmpResponseTimes.get(0));
                    }
                    sb.append(' ');
                }
                logger.info(sb.toString());
            }

            int tries = ChannelRunnable.triesCreateChannel.get();
            int created = ChannelRunnable.createdChannels.get();
            int waitingFor = tries - created;

            logger.info("active channels {}, working threads {}, thread executed {}",
                    ChannelRunnable.activeChannels, ChannelRunnable.working, ChannelRunnable.executed.get());
            logger.info("waiting for channels {}, tries to create channel {}, created channels {}",
                    waitingFor, tries, created);

            logger.info("\n");

            responseTime.clear();

        } while (all.get() < Config.instance().bombsCount);
    }

    private void end() throws InterruptedException {

        long endTime = System.currentTimeMillis();
        long workTime = (endTime - beginTime) / 1000;
        long throwoutput = all.get() / workTime;

        logger.info("Bomber was working {} seconds", workTime);
        logger.info("Throwoutput {} requests/response per second", throwoutput);

        logger.info("shutting down");

        for (ExecutorService service : executorServices) {
            service.shutdown();
        }

        for (ExecutorService service : executorServices) {
            while (!service.isTerminated()) {
                Thread.sleep(10);
            }
        }

        for (Channel channel : channels.values()) {
            channel.close().sync();
        }

        logger.info("Shut down");

    }
}
