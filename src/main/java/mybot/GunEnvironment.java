package mybot;

import org.deeplearning4j.rl4j.environment.Environment;
import org.deeplearning4j.rl4j.environment.IntegerActionSchema;
import org.deeplearning4j.rl4j.environment.Schema;
import org.deeplearning4j.rl4j.environment.StepResult;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;

import robocode.*;

import java.util.HashMap;
import java.util.Map;

public class GunEnvironment implements Environment<Integer> {

    private final AdvancedRobot robot;
    private final DiscreteSpace actionSpace = new DiscreteSpace(3); // 0: left, 1: stay, 2: right
    private final ObservationSpace<GunObservation> observationSpace =
            new ArrayObservationSpace<>(new int[]{2}); // bearing and distance

    private ScannedRobotEvent currentEnemy;
    private double reward = 0.0;
    private boolean episodeDone = false;

    public GunEnvironment(AdvancedRobot robot) {
        this.robot = robot;
    }

    public void setEnemy(ScannedRobotEvent e) {
        currentEnemy = e;
    }

    public void reward(double r) {
        reward += r;
    }

    @Override
    public Map<String, Object> reset() {
        reward = 0.0;
        episodeDone = false;
        currentEnemy = null;
        return getObservationAsMap();
    }

    // Not part of interface; do NOT use @Override
    public GunObservation getObservation() {
        double[] observationData =getObservationData();
        return new GunObservation(observationData[0], observationData[1]);
    }

    public Map<String, Object> getObservationAsMap() {
        double[] observationData = getObservationData();
        Map<String, Object> obs = new HashMap<>();
        obs.put("bearing", (observationData[0]));
        obs.put("distance", (observationData[1]));
        return obs;
    }


    private double[] getObservationData() {
        if (currentEnemy == null) {
            return new double[]{0.0, 0.0};
        }
        return new double[]{currentEnemy.getBearing(), currentEnemy.getDistance()};
    }

    @Override
    public StepResult step(Integer action) {
        // Apply the gun movement action
        if (action == 0) {
            robot.turnGunLeft(10);
        } else if (action == 2) {
            robot.turnGunRight(10);
        }

        // If no enemy detected, scan by rotating the radar
        if (currentEnemy == null) {
            robot.setTurnRadarRight(45); // Rotate radar to search for enemies
        } else {
            // Fire if enemy is detected
            robot.fire(1);
        }

        robot.execute();

        // Get the new observation
        StepResult result = new StepResult(getObservationAsMap(), reward, episodeDone);
        reward = 0.0;
        return result;
    }

    @Override
    public boolean isEpisodeFinished() {
        return episodeDone;
    }

    @Override
    public void close() {
        // Nothing to clean up
    }

    // Not part of interface; do NOT use @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }

    @Override
    public Schema<Integer> getSchema() {
        // Return both observation and action schema
        return new Schema<Integer>(
                new IntegerActionSchema(actionSpace.getSize(), 1)
        );
    }

    public void setScannedRobot(ScannedRobotEvent e) {
        setEnemy(e);
    }

    public AdvancedRobot getRobot() {
        return robot;
    }
}
