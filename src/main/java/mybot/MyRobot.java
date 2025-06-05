package mybot;

import robocode.*;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.environment.Environment;
import org.nd4j.linalg.learning.config.Adam;

import java.awt.Color;

public class MyRobot extends AdvancedRobot {

    private GunEnvironment env;
    private QLearningDiscreteDense<double[]> learner;

    @Override
    public void run() {
        setBodyColor(Color.BLUE);
        setGunColor(Color.BLACK);
        setRadarColor(Color.YELLOW);
        setScanColor(Color.RED);

        // Environment wraps Robocode APIs
        env = new GunEnvironment(this);

        // Network config
        DQNFactoryStdDense.Configuration netConf = DQNFactoryStdDense.Configuration.builder()
                .l2(0.001)
                .updater(new Adam(0.001))
                .numHiddenNodes(32)
                .numLayer(2)
                .build();

        // Q-learning configuration
        QLearning.QLConfiguration rlConf = new QLearning.QLConfiguration(
                123,    // seed
                200,    // max steps per episode
                5000,   // max episodes
                200,    // replay buffer
                32,     // batch size
                500,    // target update
                0.99,   // gamma
                1.0,    // epsilon start
                0.1,    // epsilon min
                1000    // epsilon decay
        );

        // Agent
        learner = new QLearningDiscreteDense<>(env, netConf, rlConf);

        out.println("Starting RL4J training loop...");

        while (true) {
            learner.train();
            env.reset();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        env.setScannedRobot(e);
    }

    public void onBulletHit(BulletHitEvent e) {
        env.reward(1.0);
    }

    public void onBulletMissed(BulletMissedEvent e) {
        env.reward(-0.5);
    }
}
