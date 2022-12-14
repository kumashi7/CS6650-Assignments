public class SkierVertical {
    private String seasonID;
    private int vertical;

    public SkierVertical(String seasonID, int vertical) {
        this.seasonID = seasonID;
        this.vertical = vertical;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public void setSeasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public int getVertical() {
        return vertical;
    }

    public void setVertical(int vertical) {
        this.vertical = vertical;
    }

    @Override
    public String toString() {
        return "SkierVertical{" +
                "seasonID='" + seasonID + '\'' +
                ", vertical=" + vertical +
                '}';
    }
}
