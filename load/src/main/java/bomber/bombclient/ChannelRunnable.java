package bomber.bombclient;

import bomber.config.Config;
import bomber.config.FullUri;
import bomber.config.Param;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Eugene Shurupov
 * Date: 31.07.13
 * Time: 15:00
 */
public class ChannelRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChannelRunnable.class);

    static private final Random RAND = new Random();

    static public AtomicInteger executed = new AtomicInteger(0);
    static public AtomicInteger working = new AtomicInteger(0);
    static public AtomicInteger triesCreateChannel = new AtomicInteger(0);
    static public AtomicInteger createdChannels = new AtomicInteger(0);
    static public AtomicInteger activeChannels = new AtomicInteger(0);

    private Bootstrap b;

    static {
        RAND.setSeed(System.currentTimeMillis());
    }

    public ChannelRunnable(Bootstrap bootstrap) {
        this.b = bootstrap;
    }

    @Override
    public void run() {
        try {

            executed.incrementAndGet();
            working.incrementAndGet();

            int bombsDropped = 0;

            final Object waiter = new Object();

            //Asynchronous creating channel
            triesCreateChannel.incrementAndGet();
            ChannelFuture channelFuture = b.connect(Config.instance().host, Config.instance().port).sync()
                    .addListener(new BombChannelFutureListener(waiter));

            //Waiting before channel is created
            synchronized (waiter) {
                waiter.wait();
            }

            Channel channel = channelFuture.channel();
            //Initialize channel/handler/thread parameters
            while (channel.pipeline().get(ResponseClientHandler.class) == null) {}
            ResponseClientHandler responseHandler = channel.pipeline().get(ResponseClientHandler.class);
            responseHandler.waiter = waiter;

            while (bombsDropped < Config.instance().bombsCountFromThread
                    && Bomber.instance().all.get() < Config.instance().bombsCount
                    && channel.isOpen()) {

                responseHandler.responseReceived = false;

                //Forming request
                FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                        getRandUri());
                request.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

                if (channel.isActive()) {
                    responseHandler.requestBeginTime = System.currentTimeMillis();
                    channel.writeAndFlush(request).addListener(READ);
                    bombsDropped++;
                }

                //Waiting for response
                synchronized (waiter) {
                    waiter.wait();
                    //If response is not received in time get the hell out of here
                    if (!responseHandler.responseReceived) {
                        end(channel, true, bombsDropped);
                        return;
                    }
                }

                Thread.sleep(Config.instance().delayBetweenBombs);
            }

            end(channel, false, bombsDropped);

        } catch (Exception e) {
            logger.error("Channel is broken", e);
//            end(true);
        }
    }

    private void end(Channel channel, boolean failure, int bombsDropped) {
        working.decrementAndGet();
        activeChannels.decrementAndGet();

        if (failure) {
            Bomber.instance().failed.incrementAndGet();
            logger.debug("bombs dropped {}, bad channel localAddress {}",
                    bombsDropped, channel.localAddress().toString());
        }
        if (channel != null) {
            channel.close().addListener(DEREGISTER);
        }

    }

    private static String getRandUri() {
        List<FullUri> fullUris = Config.instance().fullUris.fullUris;
        final FullUri uri;
        if (fullUris.size() == 1) {
            uri = fullUris.get(0);
        } else {
            uri = fullUris.get((int) Math.floor(RAND.nextFloat() * fullUris.size()));
        }
        List<Param> params = uri.params.params;
        final Param param;
        if (params.size() == 1) {
            param = params.get(0);
        } else {
            param = params.get((int) Math.floor(RAND.nextFloat() * params.size()));
        }
        List<String> values = param.values;
        final String paramValue = values.get((int) Math.floor(RAND.nextFloat() * values.size()));

        StringBuilder sb = new StringBuilder(uri.path);
        sb.append('?');
        sb.append(param.name);
        sb.append('=');
        sb.append(paramValue);

        logger.debug("uri = {}", sb.toString());

        return sb.toString();
    }

    final ChannelFutureListener READ = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            Bomber.instance().all.incrementAndGet();
            future.channel().read();
        }
    };

    final ChannelFutureListener DEREGISTER = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            future.channel().deregister();
        }
    };
}
