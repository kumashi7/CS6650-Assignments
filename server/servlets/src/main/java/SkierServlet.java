import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

/**
 * Class of skier servlet
 */
@WebServlet(name = "SkierServlet", value = "/skiers")
public class SkierServlet extends HttpServlet {
    private int counter = 0;
    private static final Integer URL_LENGTH = 8;
    private static final String SEASON = "seasons";
    private static final String DAY = "days";
    private static final String SKIER = "skiers";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing paramterers");
            return;
        }
        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("Good luck on your ride, skier");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        System.out.println(urlPath);

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("missing url");
            return;
        }
        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("INVALID input");
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("Good luck on your ride, skier-" + urlParts[7]);
        }
    }

    /**
     * Check if url and parameters are correct
     * @param urlParts parsed urls
     * @return true or false
     */
    private boolean isUrlValid(String[] urlParts) {
        if (urlParts.length != URL_LENGTH) {
            return false;
        }
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
        if (day != 1) {
            return false;
        }
        if (skierID < 1 || skierID > 100000) {
            return false;
        }
        if (resortID < 1 || resortID > 10) {
            return true;
        }
        return true;
    }
}
