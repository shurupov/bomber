package bomber.bombclient;

import bomber.config.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 11:14
 */
public class BombChannelFutureListener implements ChannelFutureListener {

    private static final Logger logger = LoggerFactory.getLogger(BombChannelFutureListener.class);

    private List<Channel> channels;

    public BombChannelFutureListener(List<Channel> channels) {
        this.channels = channels;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {

        channels.add(future.channel());

        logger.info("Channel created");

        if (channels.size() == Config.instance().channelCount - 1) {
            synchronized (Bomber.instance()) {
                Bomber.instance().notify();
            }
        }
    }

}
