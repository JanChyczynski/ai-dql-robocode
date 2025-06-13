package qmove;

import robocode.*;

import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class Qmove extends AdvancedRobot {
    private static final double ALPHA = 0.1;  // Learning rate
    private static final double GAMMA = 0.9;  // Discount factor
    private static final double EPSILON = 0.1;  // Exploration rate

    // State and action variables
    private final int numStates = 128; // Simplified state space
    private final int numActions = 4; // Forward, Backward, Turn Left, Turn Right
    private double[][] qTable = new double[numStates][numActions];
    private int currentState, lastState, lastAction;

    private int tick = 0;
    private int lastUpdateTick = -1000;

    private double lastEnergy = -1000;

    private int lastKnownBearing = 0;
    private int lastKnownDistance = 500;
    private int lastShotTick = 0;
    private int lastKnownHeading = 0;
    private double lastKnownSpeed = 0;

    public void run() {
        setColors(Color.RED, Color.BLACK, Color.YELLOW);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        loadQTable();

        lastState = getState();
        lastAction = chooseAction(lastState);

        //noinspection InfiniteLoopStatement
        while (true) {
            tick++;
            performAction(lastAction);
            execute();

            if (tick - lastUpdateTick < 4) {
                followWithRadar();
            } else {
                if (tick - lastUpdateTick == 4)
                    out.println("Enemy vision lost!");
                setTurnRadarRight(30);
            }

            // Update state and Q-table
            currentState = getState();
            double reward = 0; // default reward for movement
            updateQTable(lastState, lastAction, reward, currentState);

            lastState = currentState;
            lastAction = chooseAction(currentState);

        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double reward = 2;

        lastKnownBearing = (int) e.getBearing();
        lastKnownHeading = (int) e.getHeading();
        lastKnownDistance = (int) e.getDistance();
        lastKnownSpeed = e.getVelocity();

        double energy = e.getEnergy();
        if (tick - lastUpdateTick < 3) {
            if (lastEnergy - energy >= 0.6 && lastEnergy - energy <= 3) {
                lastShotTick = tick;
                out.printf("Shot detected: drop %f\n", lastEnergy - energy);
                reward = 10;
            }
        } else {
            out.println("Enemy detected!");
        }
        lastEnergy = energy;

        currentState = getState();
        updateQTable(lastState, lastAction, reward, currentState);

        lastUpdateTick = tick;

        fire(1);  // Passive fire to test movement + combat
    }

    private void followWithRadar() {
        double radarOffset = (getHeading() + lastKnownBearing - getRadarHeading() + 360) % 360;
        if (radarOffset > 180)
            radarOffset -= 360;

//        out.printf("----\nHdg: %f, Rdr: %f\n", getHeading() + lastKnownBearing, getRadarHeading());
//        out.printf("Following: %f\n", radarOffset );
        setTurnRadarRight(radarOffset);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        double reward = -30;
        currentState = getState();
        updateQTable(lastState, lastAction, reward, currentState);
    }

    public void onHitWall(HitWallEvent e) {
        double reward = -10;
        currentState = getState();
        updateQTable(lastState, lastAction, reward, currentState);
    }

    // =====================
    // Q-Learning Core Logic
    // =====================

    private int getState() {
        int bearingComponent = Math.max(0, Math.min(lastKnownBearing / 90, 3));
        int distanceComponent = Math.max(0, Math.min(lastKnownDistance / 100, 3));
        int timeComponent = Math.max(0, Math.min((tick - lastShotTick) / 5, 3));

        return (bearingComponent << 4) + (distanceComponent << 2) + timeComponent;
    }

    private int chooseAction(int state) {
        if (Math.random() < EPSILON) {
            return new Random().nextInt(numActions);
        }
        return getBestAction(state);
    }

    private int getBestAction(int state) {
        double maxQ = Double.NEGATIVE_INFINITY;
        int bestAction = 0;
        for (int a = 0; a < numActions; a++) {
            if (qTable[state][a] > maxQ) {
                maxQ = qTable[state][a];
                bestAction = a;
            }
        }
        return bestAction;
    }

    private void updateQTable(int state, int action, double reward, int nextState) {
        double maxQ = Arrays.stream(qTable[nextState]).max().getAsDouble();
        qTable[state][action] += ALPHA * (reward + GAMMA * maxQ - qTable[state][action]);
    }

    private void performAction(int action) {
        switch (action) {
            case 0:
                setAhead(30);
                break;
            case 1:
                setTurnLeft(45);
                break;
            case 2:
                setTurnRight(45);
                break;
            case 3:
                setBack(30);
                break;
        }
    }

    public void onRoundEnded(RoundEndedEvent e) {
        saveQTable();  // ← Save Q-table at the end of the battle
    }

    private void saveQTable() {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new RobocodeFileOutputStream(getDataFile("qtable1.dat")))) {
            out.writeObject(qTable);
            out.flush();
            out.close();
        } catch (IOException e) {
            out.println("Failed to save Q-table: " + e.getMessage());
        }
    }

    private void loadQTable() {
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(getDataFile("qtable1.dat")))) {
            qTable = (double[][]) in.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            out.println("No saved Q-table found or error loading it. Starting fresh.");
            qTable = new double[numStates][numActions];
        }
    }
}