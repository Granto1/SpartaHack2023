//This class defines a PointHash object so that it can be mapped with the correct color intensity
import javafx.scene.paint.Color;

public class PointHash {
    private int posx;
    private int posy;
    private int distance;

    //Uses the scaled value of the distance to plot various hues of red
    private Color distanceColor;

    //Parameterized Constructor
    public PointHash(int d, int servo, double gyro)
    {
        //Scale values to fit on Pane
        posx = servo * 4;
        posy = (int)(gyro * 200);
        distance = d;
        if (distance > Driver.max)
        {
            distance = Driver.max;
        }
        //Brighter Reds (higher values of r) are points that are closer
        int r = 255 - (int)((double)distance / Driver.cutoff * 255.0);
        distanceColor = Color.rgb(r, 0, 0);
    }

    //getters
    public int getPosy() {
        return posy;
    }

    public int getPosx() {
        return posx;
    }

    public int getDistance() {
        return distance;
    }

    public Color getDistanceColor() {
        return distanceColor;
    }
}
