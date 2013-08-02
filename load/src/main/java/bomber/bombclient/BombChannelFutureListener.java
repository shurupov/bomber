package bomber.bombclient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:14
 */
public class BombChannelFutureListener implements ChannelFutureListener {

    private static final Logger logger = LoggerFactory.getLogger(BombChannelFutureListener.class);

    private final Object waiter;
    private final Long key;
    private final Map<Long, Channel> channels;

    public BombChannelFutureListener(Object waiter, Long key, Map<Long, Channel> channels) {
        this.waiter = waiter;
        this.channels = channels;
        this.key = key;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        channels.put(key, future.channel());

        ChannelRunnable.createdChannels.incrementAndGet();

        logger.debug("Channel created. Channel pool size is {}", channels.size());

        synchronized (waiter) {
            waiter.notify();
        }

    }

}
