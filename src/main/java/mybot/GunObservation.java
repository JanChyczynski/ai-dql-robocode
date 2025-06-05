package mybot;

import org.deeplearning4j.rl4j.space.Encodable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


public class GunObservation implements Encodable {
    private final double bearing;
    private final double distance;

    public GunObservation(double bearing, double distance) {
        this.bearing = bearing;
        this.distance = distance;
    }

    @Override
    public double[] toArray() {
        return new double[]{bearing, distance};
    }

    @Override
    public INDArray getData() {
        return Nd4j.create(toArray());
    }

    @Override
    public Encodable dup() {
        return new GunObservation(bearing, distance);
    }

    @Override
    public boolean isSkipped() {
        // This is used for things like frame skipping in Atari environments.
        // Return false unless you explicitly want to skip this observation.
        return false;
    }
}
