package bomber;

import bomber.bombclient.Bomber;

/**
 * User: Eugene Shurupov
 * Date: 29.07.13
 * Time: 18:53
 */
public class App {

    public static void main(String[] args) throws InterruptedException {

        Bomber.instance().run();

    }

}
