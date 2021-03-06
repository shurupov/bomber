package bomber.bombclient;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:14
 */
public class BombChannelFutureListener implements ChannelFutureListener {

    private static final Logger logger = LoggerFactory.getLogger(BombChannelFutureListener.class);

    private final Object waiter;

    public BombChannelFutureListener(Object waiter) {
        this.waiter = waiter;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        ChannelRunnable.createdChannels.incrementAndGet();
        ChannelRunnable.activeChannels.incrementAndGet();

        logger.debug("Channel created. Channel pool size is {}", ChannelRunnable.activeChannels.get());

        synchronized (waiter) {
            waiter.notify();
        }

    }

}
