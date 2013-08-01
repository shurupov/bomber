package bomber.bombclient;

import bomber.config.Config;
import bomber.config.FullUri;
import bomber.config.Param;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * User: Eugene Shurupov
 * Date: 31.07.13
 * Time: 15:00
 */
public class ChannelRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ChannelRunnable.class);

    static private final Random RAND = new Random();

    private Bootstrap b;
    private Map<Long, Channel> channels;
    private Channel channel;


    public long key;
    public long requestBeginTime;
    public boolean responseReceived;
    public int bombsDropped = 0;

    static {
        RAND.setSeed(System.currentTimeMillis());
    }

    public ChannelRunnable(Bootstrap bootstrap, Map<Long, Channel> channels) {
        this.b = bootstrap;
        this.channels = channels;
    }

    @Override
    public void run() {
        try {

            key = RAND.nextLong();

            b.connect(Config.instance().host, Config.instance().port).sync()
                    .addListener(new BombChannelFutureListener(this, channels));

            synchronized (this) {
                wait();
            }

            channel = channels.get(key);
            channel.pipeline().get(ResponseClientHandler.class).waiter = this;

            while (bombsDropped <= Config.instance().bombsCountFromThread
                    && Bomber.instance().all.get() < Config.instance().bombsCount
                    && channel.isActive()) {

                responseReceived = false;

                FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                        getRandUri());
                request.headers().add(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

                if (channel.isActive()) {
                    requestBeginTime = System.currentTimeMillis();
                    channel.writeAndFlush(request).sync();
                    bombsDropped++;
                    Bomber.instance().all.incrementAndGet();
                }

                synchronized (this) {
                    wait(Config.instance().timeout);
                    if (!responseReceived) {
                        end(true);
                        return;
                    }
                }
            }

            end(false);

        } catch (Exception e) {
            logger.error("Channel is broken", e);
//            end(true);
        }
    }

    private void end(boolean failure) {
        if (failure) {
            Bomber.instance().failed.incrementAndGet();
        }
        try {
            if (channel != null) {
                channel.close().sync();
            }
            channels.remove(key);
        } catch (InterruptedException e) {
            logger.error("Failure close channel", e);
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
}
