package mybot;

import org.deeplearning4j.rl4j.learning.configuration.QLearningConfiguration;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.network.configuration.DQNDenseNetworkConfiguration;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import robocode.*;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.observation.Observation;
import org.nd4j.linalg.learning.config.Adam;
import org.deeplearning4j.rl4j.environment.Environment;
import org.deeplearning4j.rl4j.space.Encodable;


import java.awt.Color;

public class MyRobot extends AdvancedRobot {

    private GunEnvironment env;
    private QLearningDiscreteDense<Observation> learner;

    @Override
    public void run() {
        setBodyColor(Color.BLUE);
        setGunColor(Color.BLACK);
        setRadarColor(Color.YELLOW);
        setScanColor(Color.RED);

        env = new GunEnvironment(this);

        DQNDenseNetworkConfiguration netConf = DQNDenseNetworkConfiguration.builder()
                .l2(0.001)
                .updater(new Adam(0.001))
                .numHiddenNodes(32)
                .numLayers(2)
                .build();

        QLearningConfiguration rlConf = QLearningConfiguration.builder()
                .seed(123L)
                .maxEpochStep(200)            // max steps per episode
                .maxStep(5000)                // max episodes or max steps in total
                .expRepMaxSize(200)        // replay buffer size
                .batchSize(32)                // batch size
                .targetDqnUpdateFreq(500)   // target network update frequency
                .updateStart(10)              // number of no-op warmup steps before training starts
                .gamma(0.99)                  // discount factor
                .epsilonNbStep(1000)          // number of steps for epsilon decay
                .minEpsilon(0.1)              // final epsilon
                .build();

        Environment<Integer> gunEnvironment = new GunEnvironment(this);

        MDP<GunObservation, Integer, DiscreteSpace> mdp = new GunMDP(this);

        QLearningDiscreteDense<GunObservation> learner =
                new QLearningDiscreteDense<GunObservation>(mdp, netConf, rlConf);


        out.println("Starting RL4J training loop...");

        while (true) {
            learner.train();
            env.reset();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (env != null) {
            env.setScannedRobot(e);
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        if (env != null) {
            env.reward(1.0);
        }
    }

    @Override
    public void onBulletMissed(BulletMissedEvent e) {
        if (env != null) {
            env.reward(-0.5);
        }
    }
}
