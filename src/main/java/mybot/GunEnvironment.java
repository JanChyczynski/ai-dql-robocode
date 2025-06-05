package mybot;

import org.deeplearning4j.rl4j.environment.Environment;
import org.deeplearning4j.rl4j.environment.IntegerActionSchema;
import org.deeplearning4j.rl4j.environment.Schema;
import org.deeplearning4j.rl4j.environment.StepResult;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import robocode.*;

import java.util.HashMap;
import java.util.Map;

// This should not be public!
class GunEnvironment implements Environment<Integer> {

    private final AdvancedRobot robot;
    private final DiscreteSpace actionSpace = new DiscreteSpace(3); // 0: left, 1: stay, 2: right
    private final ObservationSpace<Map<String, Object>> observationSpace =
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
        return getObservation();
    }

    // Not part of interface; do NOT use @Override
    public Map<String, Object> getObservation() {
        INDArray input = Nd4j.create(getObservationData());
        Map<String, Object> obs = new HashMap<>();
        obs.put("data", input);
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
        if (action == 0) robot.turnGunLeft(10);
        else if (action == 2) robot.turnGunRight(10);

        if (currentEnemy != null) {
            robot.fire(1);
        }

        robot.execute();

        StepResult result = new StepResult(getObservation(), reward, episodeDone);
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
                observationSpace,
                new IntegerActionSchema(actionSpace.getSize(), TODO add mising int noOpAction argument),
                false // not recurrent
//        TODO are you sure it needs any other ctor arguments than IntegerActionSchema
        );
    }
}
