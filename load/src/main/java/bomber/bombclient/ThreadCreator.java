package bomber.bombclient;

import bomber.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

import java.util.Map;

/**
 * User: Eugene Shurupov
 * Date: 01.08.13
 * Time: 12:53
 */
public class ThreadCreator implements Runnable {

    private Map<Long, Channel> channels;
    private Bootstrap bootstrap;

    public ThreadCreator(Map<Long, Channel> channels, Bootstrap bootstrap) {
        this.channels = channels;
        this.bootstrap = bootstrap;
    }

    @Override
    public void run() {

        for (int i = 0; i < Config.instance().channelCount; i++) {
            /*try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }*/
            createBombThread();
        }

        while (Bomber.instance().all.get() < Config.instance().bombsCount) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            if (channels.size() < Config.instance().channelCount) {
                createBombThread();
            }
        }

    }

    private void createBombThread() {
        new Thread(new ChannelRunnable(bootstrap, channels)).start();
    }

}
