package bomber.config;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

/**
 * User: Eugene Shurupov
 * Date: 30.07.13
 * Time: 12:57
 */
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static Config config = newInstance();

    public String host;
    public int port;
    public int threadCount;
    public int threadRelaxTime;
    public int threadsIncreaseDelay;
    public long bombsCount;
    public long delayBetweenBombs;
    public long timeout;
    public int bombsCountFromThread;

    public FullUris fullUris;

    private static Config newInstance() {

        XStream xStream = new XStream();
        xStream.alias("app", Config.class);
        xStream.addImplicitCollection(FullUris.class, "fullUris", "fullUri", FullUri.class);
        xStream.addImplicitCollection(Params.class, "params", "param", Param.class);

        String path = System.getProperty("user.dir") + File.separator + "settings.xml";
        logger.debug("config file path {}", path);

        Config instance = (Config) xStream.fromXML(new File(path));

        for (FullUri uri : instance.fullUris.fullUris) {
            if (uri.params != null
                    && uri.params.params != null
                    && !uri.params.params.isEmpty()) {
                for (Param param : uri.params.params) {
                    if (StringUtils.isNotBlank(param.file)) {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(param.file))));
                            param.values = new ArrayList<>();
                            while (reader.ready()) {
                                param.values.add(reader.readLine());
                            }
                        } catch (Exception e) {
                            logger.debug("param values are not read", e);
                        }
                    }
                }
            }
        }

        return instance;

    }

    public static Config instance() {
        return config;
    }

}
