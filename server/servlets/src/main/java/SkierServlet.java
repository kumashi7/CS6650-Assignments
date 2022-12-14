import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Class of skier servlet
 */
@WebServlet(name = "SkierServlet", value = "/skiers")
public class SkierServlet extends HttpServlet {
    private Gson gson  = new Gson();
    private static final String SEASON = "seasons";
    private static final String DAY = "days";
    private static final String SKIER = "skiers";

//    private static final int numOfThreads = 16;

    private static int counter = 0;

    private ObjectPool<Channel> channelObjectPool;

    private final static String QUEUE_NAME = "threadExQ";

    @Override
    public void init() throws ServletException {
        channelObjectPool = new GenericObjectPool<>(new ChannelFactory());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

//        Jedis jedis = new Jedis("http://35.88.86.7:6379");
        JedisPool jedisPool = new JedisPool("52.34.15.82", 6379);
        Jedis jedis = jedisPool.getResource();
        String[] urlParts = urlPath.split("/");
        Map<String, String> skierFields;

        if (!isUrlValid(urlParts, req)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("INVALID input");
        } else if (urlParts[2].equals("vertical")) {
            // GET/skiers/{skierID}/vertical
//            SkierVertical skierVertical = new SkierVertical(urlParts[1], 10);
//            res.getWriter().write(gson.toJson(skierVertical));
            int totalVertical = 0;
            String skierID = urlParts[1];
            skierFields = jedis.hgetAll(skierID);
            if (!skierFields.isEmpty()) {
                for (String key :skierFields.keySet()) {
                    totalVertical += Integer.parseInt(skierFields.get(key));
                }
            }
            if (totalVertical != 0) {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().write("Total vertical for skier " + skierID
                        + " in current season is: " + totalVertical);
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write("Cannot find any record for skier " + skierID);
            }
        }
        //res.getWriter().write("Good luck on your ride, skier");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("missing url");
            return;
        }
        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts, req)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("INVALID input " + urlPath);
        } else {
            try {
                Channel channel = channelObjectPool.borrowObject();
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                channel.basicPublish("", QUEUE_NAME, null, urlPath.getBytes(StandardCharsets.UTF_8));
                channelObjectPool.returnObject(channel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("Good luck on your ride, skier-" + urlParts[7]);
        }
    }

    /**
     * Check if url and parameters are correct
     * @param urlParts parsed urls
     * @return true or false
     */
    private boolean isUrlValid(String[] urlParts, HttpServletRequest req) {
        // skiers/12/seasons/2019/day/1/skier/123
        if (urlParts.length == 8) {
            // validate parameters
            if (!SEASON.equals(urlParts[2]) || !DAY.equals(urlParts[4]) || !SKIER.equals(urlParts[6])) {
                return false;
            }
            int day;
            int skierID;
            int resortID;
            try {
                resortID = Integer.parseInt(urlParts[1]);
                day = Integer.parseInt(urlParts[5]);
                skierID = Integer.parseInt(urlParts[7]);
            } catch (NumberFormatException e) {
                return false;
            }
            if (day != 1 && day != 2 && day != 3) {
                return false;
            }
            if (skierID < 1 || skierID > 100000) {
                return false;
            }
            if (resortID < 1 || resortID > 10) {
                return false;
            }
            return true;
        } else if (urlParts.length == 3) {
            Integer.parseInt(urlParts[1]);
            return (urlParts[2].equals("vertical"));
        } else {
            return false;
        }
    }
}
