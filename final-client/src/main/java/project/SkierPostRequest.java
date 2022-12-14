package project;

import io.swagger.client.model.LiftRide;

import java.util.Random;

import static project.Constants.*;

/**
 * Class of skier post request objects
 */
public class SkierPostRequest {
    public LiftRide liftRide;
    public int resortID;
    public String seasonID;
    public String dayID;
    public int skierID;

    public SkierPostRequest() {
        Random r = new Random();
//        int skierID = r.nextInt(MAX_SKIERID) + 1;
//        int resortID = r.nextInt(MAX_RESORTID) + 1;
        int skierID = 1;
        int resortID = 1;
        String dayID = "1";
        String seasonID = "2022";

        LiftRide liftRide = new LiftRide();
        int liftID = r.nextInt(MAX_LIFTID) + 1;
        int liftTime = r.nextInt(MAX_LIFTTIME) + 1;
        liftRide.setLiftID(liftID);
        liftRide.setTime(liftTime);

        this.liftRide = liftRide;
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
    }

    @Override
    public String toString() {
        return "client1.SkierPostRequest{" +
                "liftRide=" + liftRide +
                ", resortID=" + resortID +
                ", seasonID='" + seasonID + '\'' +
                ", dayID='" + dayID + '\'' +
                ", skierID=" + skierID +
                '}';
    }
}
