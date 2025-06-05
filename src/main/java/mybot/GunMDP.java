package mybot;

import mybot.GunEnvironment;
import mybot.GunObservation;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.environment.StepResult;
import robocode.AdvancedRobot;

public class GunMDP implements MDP<GunObservation, Integer, DiscreteSpace> {

    private final GunEnvironment environment;
    private final ObservationSpace<GunObservation> observationSpace = new ArrayObservationSpace<>(new int[]{2});
    private final DiscreteSpace actionSpace = new DiscreteSpace(3);

    public GunMDP(AdvancedRobot robot) {
        this.environment = new GunEnvironment(robot);
    }

    @Override
    public ObservationSpace<GunObservation> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }

    @Override
    public GunObservation reset() {
        return new GunObservation(0.0, 0.0);
    }

    @Override
    public void close() {
        environment.close();
    }

    @Override
    public StepReply<GunObservation> step(Integer action) {
        StepResult result = environment.step(action);
        GunObservation gunObservation = new GunObservation((double)result.getChannelsData().get("bearing"),
                (double)result.getChannelsData().get("distance"));

        return new StepReply<GunObservation>(environment.getObservation(), result.getReward(), result.isTerminal(), null);
    }

    @Override
    public boolean isDone() {
        return environment.isEpisodeFinished();
    }

    @Override
    public MDP<GunObservation, Integer, DiscreteSpace> newInstance() {
        return new GunMDP(environment.getRobot());
    }
}
