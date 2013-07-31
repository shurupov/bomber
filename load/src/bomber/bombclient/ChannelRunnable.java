package bomber.bombclient;

import bomber.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Eugene Shurupov
 * Date: 31.07.13
 * Time: 15:00
 */
public class ChannelRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChannelRunnable.class);

    private Bootstrap b;
    public int i;

    public ChannelRunnable(Bootstrap b) {
        this.b = b;
    }

    @Override
    public void run() {
        try {
            b.connect(Config.instance().host, Config.instance().port).sync()
                    .addListener(new BombChannelFutureListener(this));

            synchronized (this) {
                wait();
            }

            Channel channel = Bomber.instance().channels.get(i);
            channel.pipeline().get(ResponseClientHandler.class).waiter = this;

            while (Bomber.instance().all.get() < Config.instance().bombsCount) {

                FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                        Bomber.instance().getRandUri());
                request.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

                if (channel.isActive()) {
                    channel.writeAndFlush(request).sync();
                    Bomber.instance().all.incrementAndGet();
                }

                synchronized (this) {
                    wait();
                }
            }

        } catch (Exception e) {
            logger.error("Channel is broken {}", e.getCause().toString());
        }
    }
}
