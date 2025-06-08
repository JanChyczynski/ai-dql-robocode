package mybot;

import robocode.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

//import static jdk.javadoc.internal.tool.Main.execute;

public class MyRobot extends AdvancedRobot {
    private static final int NUM_DISTANCE_BUCKETS = 5;
    private static final int NUM_BEARING_BUCKETS = 8;
    private static final int NUM_ACTIONS = 5;

    private static final double ALPHA = 0.1;
    private static final double GAMMA = 0.9;
    private static final double EPSILON = 0.1;

    private double[][][] qTable = new double[NUM_DISTANCE_BUCKETS][NUM_BEARING_BUCKETS][NUM_ACTIONS];
    private int prevDistance, prevBearing, prevAction;
    private boolean hasPrevState = false;
    private final String Q_TABLE_FILE = "qtable.data";

    @Override
    public void run() {
        loadQTable();
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            turnRadarRight(360);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        int dist = (int) Math.min(NUM_DISTANCE_BUCKETS - 1, e.getDistance() / (1000 / NUM_DISTANCE_BUCKETS));
        int bearing = (int) Math.floor((e.getBearing() + 180) / (360.0 / NUM_BEARING_BUCKETS));

        int action = chooseAction(dist, bearing);
        performAction(action);

        double reward = 0;
        if (e.getEnergy() < 20) reward += 1;
        if (getEnergy() < 20) reward -= 1;

        if (hasPrevState) {
            double oldQ = qTable[prevDistance][prevBearing][prevAction];
            double maxQ = Arrays.stream(qTable[dist][bearing]).max().orElse(0);
            qTable[prevDistance][prevBearing][prevAction] = oldQ + ALPHA * (reward + GAMMA * maxQ - oldQ);
        }

        prevDistance = dist;
        prevBearing = bearing;
        prevAction = action;
        hasPrevState = true;

        scan();
    }

    private int chooseAction(int dist, int bearing) {
        if (Math.random() < EPSILON) {
            return (int)(Math.random() * NUM_ACTIONS);
        }
        return maxQIndex(dist, bearing);
    }

    private int maxQIndex(int dist, int bearing) {
        double[] q = qTable[dist][bearing];
        int best = 0;
        for (int i = 1; i < q.length; i++) {
            if (q[i] > q[best]) best = i;
        }
        return best;
    }

    private void performAction(int action) {
        switch (action) {
            case 0: setAhead(100); break;
            case 1: setBack(100); break;
            case 2: setTurnRight(45); break;
            case 3: setTurnLeft(45); break;
            case 4: setFire(1.5); break;
        }
        execute();
    }

    @Override
    public void onWin(WinEvent e) {
        out.println("Victory! Saving Q-table...");
        saveQTable();
    }

    @Override
    public void onDeath(DeathEvent e) {
        out.println("Defeated. Saving Q-table...");
        saveQTable();
    }

    private void saveQTable() {
        try (ObjectOutputStream out = new ObjectOutputStream(new RobocodeFileOutputStream(getDataFile(Q_TABLE_FILE)))) {
            out.writeObject(qTable);
            out.flush();
        } catch (IOException ex) {
            out.println("Failed to save Q-table: " + ex.getMessage());
        }
    }

    private void loadQTable() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(getDataFile(Q_TABLE_FILE)))) {
            Object obj = in.readObject();
            if (obj instanceof double[][][]) {
                qTable = (double[][][]) obj;
                out.println("Loaded Q-table.");
            }
        } catch (IOException | ClassNotFoundException ex) {
            out.println("No existing Q-table found, starting fresh.");
        }
    }
}
