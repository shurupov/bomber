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

    private ChannelRunnable waiter;

    public BombChannelFutureListener(ChannelRunnable waiter) {
        this.waiter = waiter;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        Bomber.instance().channels.add(future.channel());

        logger.info("Channel created");

        synchronized (waiter) {
            waiter.notify();
            waiter.i = Bomber.instance().channels.indexOf(future.channel());
        }

    }

}
