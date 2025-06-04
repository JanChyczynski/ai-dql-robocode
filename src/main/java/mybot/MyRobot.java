package mybot;

import robocode.*;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.network.configuration.DQNFactoryStdDense.Configuration;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.environment.Environment;
import org.deeplearning4j.rl4j.environment.StepResult;
import org.deeplearning4j.rl4j.environment.EnvironmentStep;
import org.deeplearning4j.rl4j.space.Observation;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.environment.EnvironmentListener;
import org.nd4j.linalg.learning.config.Adam;

import java.util.*;

public class MyRobot extends AdvancedRobot {

    private SimpleGunEnv env;
    private QLearningDiscreteDense<double[]> dql;

    @Override
    public void run() {
        env = new SimpleGunEnv(this);

        Configuration netConf = DQNFactoryStdDense.Configuration.builder()
                .l2(0.001).updater(new Adam(0.001))
                .numHiddenNodes(32)
                .numLayer(2)
                .build();

        QLearning.QLConfiguration rlConf = new QLearning.QLConfiguration(
                123,    // Random seed
                200,    // Max steps per episode
                5000,   // Max episodes
                200,    // Exp replay size
                32,     // Batch size
                500,    // Target update (steps)
                0.99,   // Gamma
                1.0,    // Initial epsilon
                0.1,    // Min epsilon
                1000    // Exploration anneal steps
        );

        dql = new QLearningDiscreteDense<>(env, netConf, rlConf);

        while (true) {
            dql.train();
            env.reset();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        env.setEnemy(e);
    }

    // Optional: reward logic after a hit or miss
    public void onBulletHit(BulletHitEvent e) {
        env.reward(1.0);
    }

    public void onBulletMissed(BulletMissedEvent e) {
        env.reward(-0.2);
    }
}
