package mybot;

import robocode.*;
import java.io.*;
import java.util.Arrays;

public class MyRobot extends AdvancedRobot {

    // Fields to track stats
    private double totalReward = 0;
    private double totalDamage = 0;
    private int hits = 0;
    private int misses = 0;
    private static String logFile;
    private boolean logInitialized = false;

    private static final String TYPE_NAME = "stationaryV1";
    private static final int NUM_DISTANCE_BUCKETS = 5;
    private static final int NUM_ANGLE_BUCKETS = 9;  // Angle difference between gun and enemy
    private static final int NUM_ACTIONS = 3; // 0: turn left, 1: turn right, 2: fire

    private static final double ALPHA = 0.1;
    private static final double GAMMA = 0.9;
    private static final double EPSILON = 0.1;

    private final String PARAMETERS = String.format("A%.2f_G%.2f_E%.2f_BD%d_BA%d",
            ALPHA, GAMMA, EPSILON, NUM_DISTANCE_BUCKETS, NUM_ANGLE_BUCKETS);

    private final String Q_TABLE_FILE = "qtable_gun.data";

    private double[][][] qTable = new double[NUM_DISTANCE_BUCKETS][NUM_ANGLE_BUCKETS][NUM_ACTIONS];
    private int prevDist, prevAngle, prevAction;
    private boolean hasPrevState = false;

    public void run() {
        logFile = "unprocessed_" + TYPE_NAME + "_" + PARAMETERS + ".log";

        loadQTable();
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            turnRadarRight(360); // spin to scan for enemies
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        int distBucket = (int) Math.min(NUM_DISTANCE_BUCKETS - 1, e.getDistance() / (1000 / NUM_DISTANCE_BUCKETS));
        double angleToEnemy = normalRelativeAngleDegrees(getHeading() + e.getBearing() - getGunHeading());
        int angleBucket = (int) Math.floor((angleToEnemy + 180) / (360.0 / NUM_ANGLE_BUCKETS));
        angleBucket = Math.max(0, Math.min(NUM_ANGLE_BUCKETS - 1, angleBucket));

        int action = chooseAction(distBucket, angleBucket);
        performAction(action, angleToEnemy);

        double reward = 0;
        if (getGunHeat() > 0 && getEnergy() < 100) reward -= 0.1; // punish for useless firing
        if (e.getEnergy() < 10) reward += 1.0; // reward when enemy is weak

        if (hasPrevState) {
            double oldQ = qTable[prevDist][prevAngle][prevAction];
            double maxQ = Arrays.stream(qTable[distBucket][angleBucket]).max().orElse(0);
            qTable[prevDist][prevAngle][prevAction] = oldQ + ALPHA * (reward + GAMMA * maxQ - oldQ);
        }
        totalReward += reward;

        prevDist = distBucket;
        prevAngle = angleBucket;
        prevAction = action;
        hasPrevState = true;

        scan(); // continue scanning
    }

    public void onBulletHit(BulletHitEvent e) {
        hits++;
        totalDamage += e.getBullet().getPower() * 4;
    }

    public void onBulletMissed(BulletMissedEvent e) {
        misses++;
    }

    public void onWin(WinEvent e) {
        logRoundStats(1);
        saveQTable();
        resetStats();
    }

    public void onDeath(DeathEvent e) {
        logRoundStats(0);
        saveQTable();
        resetStats();
    }

    private void logRoundStats(int win) {
        int totalShots = hits + misses;
        double accuracy = (totalShots > 0) ? ((double) hits / totalShots) : 0.0;

        try {
            File logDataFile = getDataFile(logFile);
            boolean fileExists = logDataFile.exists();

            try (PrintWriter writer = new PrintWriter(new FileWriter(logDataFile, true))) {
                if (!fileExists) {
                    writer.println("reward,damage,accuracy,win");
                }
                writer.printf("%.2f,%.2f,%.4f,%d%n", totalReward, totalDamage, accuracy, win);
            }
        } catch (IOException e) {
            out.println("Failed to write log: " + e.getMessage());
        }
    }

    private void resetStats() {
        totalReward = 0;
        totalDamage = 0;
        hits = 0;
        misses = 0;
    }

    private int chooseAction(int dist, int angle) {
        if (Math.random() < EPSILON) {
            return (int)(Math.random() * NUM_ACTIONS);
        }
        return maxQIndex(dist, angle);
    }

    private int maxQIndex(int dist, int angle) {
        double[] q = qTable[dist][angle];
        int best = 0;
        for (int i = 1; i < q.length; i++) {
            if (q[i] > q[best]) best = i;
        }
        return best;
    }

    private void performAction(int action, double angleToEnemy) {
        switch (action) {
            case 0: setTurnGunLeft(Math.min(10, Math.abs(angleToEnemy))); break;
            case 1: setTurnGunRight(Math.min(10, Math.abs(angleToEnemy))); break;
            case 2:
                if (Math.abs(angleToEnemy) < 5) {
                    setFire(2);
                }
                break;
        }
        execute();
    }

    private void saveQTable() {
        try (ObjectOutputStream out = new ObjectOutputStream(new RobocodeFileOutputStream(getDataFile(Q_TABLE_FILE)))) {
            out.writeObject(qTable);
        } catch (IOException ex) {
            out.println("Failed to save Q-table: " + ex.getMessage());
        }
    }

    private void loadQTable() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(getDataFile(Q_TABLE_FILE)))) {
            Object obj = in.readObject();
            if (obj instanceof double[][][]) {
                qTable = (double[][][]) obj;
                out.println("Q-table loaded.");
            }
        } catch (IOException | ClassNotFoundException ex) {
            out.println("No existing Q-table found, starting fresh.");
        }
    }

    private double normalRelativeAngleDegrees(double angle) {
        while (angle <= -180) angle += 360;
        while (angle > 180) angle -= 360;
        return angle;
    }
}