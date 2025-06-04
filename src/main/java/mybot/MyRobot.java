package mybot;

import robocode.*;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
//import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.nd4j.linalg.learning.config.Adam;

import java.awt.Color;

public class MyRobot extends AdvancedRobot {

    @Override
    public void run() {
        setBodyColor(Color.BLUE);
        setGunColor(Color.BLACK);
        setRadarColor(Color.YELLOW);
        setScanColor(Color.RED);

        out.println("Initializing DQN...");

        // Just instantiating RL4J classes to verify setup
        DQNFactoryStdDense.Configuration netConf = DQNFactoryStdDense.Configuration.builder()
                .l2(0.001)
                .updater(new Adam(0.001))
                .numHiddenNodes(16)
                .numLayer(2)
                .build();

        out.println("RL4J DQN config instantiated OK");

        // Dummy loop
        while (true) {
            turnGunRight(10); // just to show something
            execute();
        }
    }
}
