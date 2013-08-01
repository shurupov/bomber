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

    private final ChannelRunnable waiter;
    private final Map<Long, Channel> channels;

    public BombChannelFutureListener(ChannelRunnable waiter, Map<Long, Channel> channels) {
        this.waiter = waiter;
        this.channels = channels;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        channels.put(waiter.key, future.channel());

        logger.debug("Channel created. Channel pool size is {}", channels.size());

        synchronized (waiter) {
            waiter.notify();
        }

    }

}
