package mybot;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

// DL4J imports
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MyRobot extends AdvancedRobot {
    @Override
    public void run() {
        // Simple DL4J NN configuration example
        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(5)
                        .nOut(10)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(10)
                        .nOut(1)
                        .build());

        System.out.println("Created a simple DL4J neural network config");

        while (true) {
//            ahead(100);
            turnGunRight(360);
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        fire(1);
    }
}
