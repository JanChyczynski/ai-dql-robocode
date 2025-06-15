package forkbot; //change it into your package name

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.*;

import robocode.*;

import java.util.Random;

public class ForkBot extends AdvancedRobot {
    double cum_reward_while = 0;
    static double[] cum_reward_array = new double[40000];
    static int index1 = 0;

    static Random rand = new Random();
    final double alpha = 0.1;
    final double gamma = 0.9;
    double distance = 0;

    //declaring actions
    int[] action = new int[4];

    //LUT table initialization
    int[] total_states_actions = new int[8 * 6 * 4 * 4 * action.length];
    int[] total_actions = new int[4];
    String[][] LUT = new String[total_states_actions.length][2];
    double[][] LUT_double = new double[total_states_actions.length][2];

    //quantized parameters
    int qrl_x = 0;
    int qrl_y = 0;
    private RobotStatus robotStatus;
    int qheading = 0;

    double absbearing = 0;
    int q_absbearing = 0;

    //initialize reward
    double reward = 0;
    String state_action_combi = null;
    String state_action_combi_greedy = null;
    double robot_energy = 0;
    int sa_combi_inLUT = 0;

    int qenemy_x = 0;
    int qenemy_y = 0;


    //Run command-Robocode
    String q_present = null;
    double q_present_double = 0;
    int random_action = 0;
    String state_action_combi_next = null;
    int sa_combi_inLUT_next = 0;
    String q_next = null;
    double q_next_double = 0;
    int Qmax_action = 0;
    int[] actions_indices = new int[total_actions.length];
    double[] q_possible = new double[total_actions.length];
    int Qmax_actual_action = 0;
    double my_energy_pres = 0;
    double my_energy_next = 0;
    double gunTurnTowardEnemyNeeded;

    private double enemyHeadingRadians;
    private double enemyVelocity;
    private double enemyAbsBearingRadians;
    private double enemyBearing;
    private double time;
    private double normalizedBearing;

    // Fields to track stats
//    private double totalReward = 0;
    private double totalDamage = 0;
    private double totalDamageTaken = 0;
    private static String logFile;
    private static String headerWrittenFile;

    private static final String TYPE_NAME = "orgV1";
    private final String PARAMETERS = String.format("A%.2f_G%.2f_E%.2f",
            alpha, gamma, 0.5);


    //-------------Explore or greedy----------------------//
    boolean explore = true;
    boolean greedy = true;
    //----------------------------------------------------//

    boolean seenSinceLastCheck = false;


    public void run() {
        logFile = "unprocessed_" + TYPE_NAME + "_" + PARAMETERS + ".log";
        headerWrittenFile = logFile + ".header_written.tmp";

        setColors(null, new Color(192, 192, 192), new Color(192, 192, 192), Color.black, new Color(150, 0, 150));
        setBodyColor(new java.awt.Color(192, 192, 192, 100));

        setAdjustGunForRobotTurn(true);

        initialiseLUT();
        // saveLookUpTable();
        try {
            loadLookUpTable();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //noinspection InfiniteLoopStatement
        while (true) {
            qheading = quantize_angle(getHeading());
            qrl_x = quantize_position(getX());
            qrl_y = quantize_position(getY());

            if (explore) {
                if (!seenSinceLastCheck)
                    turnGunRight(360);
                seenSinceLastCheck = false;

                random_action = randInt(1, total_actions.length);
                state_action_combi = "" + qrl_x + qrl_y + qheading + q_absbearing + random_action;
                for (int i = 0; i < LUT.length; i++) {
                    if (LUT[i][0].equals(state_action_combi)) {
                        sa_combi_inLUT = i;
                        break;
                    }
                }
                q_present = LUT[sa_combi_inLUT][1];
                q_present_double = Double.parseDouble(q_present);

                //performing next state and scanning
                my_energy_pres = robot_energy;

                makeAction(random_action);
                execute();
                //turnGunRight(360);

                my_energy_next = robot_energy;
                reward = (my_energy_next - my_energy_pres);

                state_action_combi_next = "" + qrl_x + qrl_y + qheading + q_absbearing + random_action;
                for (int i = 0; i < LUT.length; i++) {
                    if (LUT[i][0].equals(state_action_combi_next)) {
                        sa_combi_inLUT_next = i;
                        break;
                    }
                }
                q_next = LUT[sa_combi_inLUT_next][1];
                q_next_double = Double.parseDouble(q_next);

                //performing update
                q_present_double = q_present_double + alpha * (reward + gamma * q_next_double - q_present_double);
                LUT[sa_combi_inLUT][1] = Double.toString(q_present_double);
                cum_reward_while += reward;
            }

            /*
             *
            GREEDY
             *
             */

            if (greedy) {
                if (!seenSinceLastCheck)
                    turnGunRight(360);
                seenSinceLastCheck = false;

                // finding action that produces maximum Q value
                for (int j = 1; j <= total_actions.length; j++) {
                    state_action_combi = "" + qrl_x + qrl_y + qheading + q_absbearing + j;
                    for (int i = 0; i < LUT.length; i++) {
                        if (LUT[i][0].equals(state_action_combi)) {
                            actions_indices[j - 1] = i;
                            break;
                        }
                    }
                }

                // converting table to double
                for (int i = 0; i < total_states_actions.length; i++) {
                    for (int j = 0; j < 2; j++) {
                        LUT_double[i][j] = Double.valueOf(LUT[i][j]).doubleValue();
                    }
                }

                //converting table to double
                for (int k = 0; k < total_actions.length; k++) {
                    q_possible[k] = LUT_double[actions_indices[k]][1];
                }

                Qmax_action = maxIndex(q_possible) + 1;

                //find position of actions
                for (int i = 0; i < 4; i++) {
                    if (actions_indices[i] == Qmax_action) {
                        Qmax_actual_action = i + 1;
                    }
                }

                //finding action that produces maximum q
                state_action_combi_greedy = "" + qrl_x + qrl_y + qheading + q_absbearing + Qmax_action;

                for (int i = 0; i < LUT.length; i++) {
                    if (LUT[i][0].equals(state_action_combi_greedy)) {
                        sa_combi_inLUT = i;
                        break;
                    }
                }

                q_present = LUT[sa_combi_inLUT][1];
                q_present_double = Double.parseDouble(q_present);

                //performing next state and scanning
                my_energy_pres = robot_energy;

                makeAction(Qmax_action);
                execute();
                //turnGunRight(360);

                my_energy_next = robot_energy;
                reward = (my_energy_next - my_energy_pres);

                state_action_combi_next = "" + qrl_x + qrl_y + qheading + q_absbearing + Qmax_action;
                for (int i = 0; i < LUT.length; i++) {
                    if (LUT[i][0].equals(state_action_combi_next)) {
                        sa_combi_inLUT_next = i;
                        break;
                    }
                }
                q_next = LUT[sa_combi_inLUT_next][1];
                q_next_double = Double.parseDouble(q_next);

                //performing update
                q_present_double = q_present_double + alpha * (reward + gamma * q_next_double - q_present_double);
                LUT[sa_combi_inLUT][1] = Double.toString(q_present_double);
                cum_reward_while += reward;
            }

            if (reward != 0) {
                out.println(reward);
            }
        }
    }


    public void onScannedRobot(ScannedRobotEvent e) {
        enemyAbsBearingRadians = e.getBearingRadians() + getHeadingRadians();
        enemyVelocity = e.getVelocity();
        enemyHeadingRadians = e.getHeadingRadians();
        enemyBearing = e.getBearing();
        time = getTime();
        gunTurnTowardEnemyNeeded = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
        normalizedBearing = normalizeBearing(enemyBearing + 75); // ????

        robot_energy = getEnergy();
        distance = e.getDistance();

        int qdistancetoenemy = quantizeDistance(distance);

        if (qdistancetoenemy == 1) {
            fire(3);
        }
        if (qdistancetoenemy == 2) {
            fire(2);
        }
        if (qdistancetoenemy == 3) {
            fire(1);
        }

        //Calculating Enemy X & Y:
        double angleToEnemy = e.getBearing();
        double angle = Math.toRadians((getHeading() + angleToEnemy % 360));
        double enemyX = (getX() + Math.sin(angle) * e.getDistance());
        double enemyY = (getY() + Math.cos(angle) * e.getDistance());

        //absolute angle to enemy
        absbearing = absoluteBearing((float) getX(), (float) getY(), (float) enemyX, (float) enemyY);
        q_absbearing = quantize_angle(absbearing);

        seenSinceLastCheck = true;
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
    }


    /*
    Events
     */

    public void onHitRobot(HitRobotEvent event) {
        reward -= 2;
    }

    public double powerToDamage(double power) {
//        4 * power, plus 2 * (power-1) if power > 1
        return 4 * power + ((power > 1) ? 2 * (power-1) : 0);
    }


    public void onBulletHit(BulletHitEvent event) {
        reward += 3;
        totalDamage += powerToDamage(event.getBullet().getPower());
    } //one of our bullet hits enemy robot

    public void onHitByBullet(HitByBulletEvent event) {
        totalDamageTaken += powerToDamage(event.getBullet().getPower());
        reward -= 3;
    }

    // FWIK this turns away from a wall after hitting it
    public void onHitWall(HitWallEvent e) {
        reward -= 3.5;
        double xPos = this.getX();
        double yPos = this.getY();
        double width = this.getBattleFieldWidth();
        double height = this.getBattleFieldHeight();
        if (yPos < 80) //too close to the bottom
        {
            turnLeft(getHeading() % 90);
            if (getHeading() == 0) {
                turnLeft(0);
            }
            if (getHeading() == 90) {
                turnLeft(90);
            }
            if (getHeading() == 180) {
                turnLeft(180);
            }
            if (getHeading() == 270) {
                turnRight(90);
            }
            ahead(150);
            //System.out.println("Too close to the bottom");
            if ((this.getHeading() < 180) && (this.getHeading() > 90)) {
                this.setTurnLeft(90);
            } else if ((this.getHeading() < 270) && (this.getHeading() > 180)) {
                this.setTurnRight(90);
            }


        } else if (yPos > height - 80) { //to close to the top
            //System.out.println("Too close to the Top");
            if ((this.getHeading() < 90) && (this.getHeading() > 0)) {
                this.setTurnRight(90);
            } else if ((this.getHeading() < 360) && (this.getHeading() > 270)) {
                this.setTurnLeft(90);
            }
            turnLeft(getHeading() % 90);
            //System.out.println("Get heading");
            //System.out.println(getHeading());
            if (getHeading() == 0) {
                turnRight(180);
            }
            if (getHeading() == 90) {
                turnRight(90);
            }
            if (getHeading() == 180) {
                turnLeft(0);
            }
            if (getHeading() == 270) {
                turnLeft(90);
            }
            ahead(150);

        } else if (xPos < 80) {
            turnLeft(getHeading() % 90);
            //System.out.println("Get heading");
            //System.out.println(getHeading());
            if (getHeading() == 0) {
                turnRight(90);
            }
            if (getHeading() == 90) {
                turnLeft(0);
            }
            if (getHeading() == 180) {
                turnLeft(90);
            }
            if (getHeading() == 270) {
                turnRight(180);
            }
            ahead(150);
        } else if (xPos > width - 80) {
            turnLeft(getHeading() % 90);
            //System.out.println("Get heading");
            //System.out.println(getHeading());
            if (getHeading() == 0) {
                turnLeft(90);
            }
            if (getHeading() == 90) {
                turnLeft(180);
            }
            if (getHeading() == 180) {
                turnRight(90);
            }
            if (getHeading() == 270) {
                turnRight(0);
            }
            ahead(150);
        }
    }

    @Override
    public void onRoundEnded(RoundEndedEvent e) {
        System.out.println("cumulative reward of one full battle is ");
        System.out.println(cum_reward_while);
        System.out.println("index number ");
        System.out.println(getRoundNum());
        cum_reward_array[getRoundNum()] = cum_reward_while;



        index1 = index1 + 1;

        if (getRoundNum() % 1000 == 0) {
            for (int i = 0; i < getRoundNum(); i++) {
                out.println(cum_reward_array[i]);
                out.println();
            }
        }

        saveLookUpTable();
    }

    public void onBattleEnded(BattleEndedEvent e) {
        saveLookUpTable();
        saveCumulative();
    }

    /*
    Math methods
     */

    public double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private int quantize_angle(double absbearing2) {
        int b = (int) Math.min(Math.max(absbearing2, 0), 360);
        q_absbearing = b / 90 + 1;
        return q_absbearing;
    }

    private int quantizeDistance(double distance2) {
        int d = (int) Math.max(Math.min(distance2, 1000), 0);
        return d / 250 + 1;
    }

    double absoluteBearing(float x1, float y1, float x2, float y2) {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double hyp = Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actuall 360 - ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
        }

        return bearing;
    }

    private int quantize_position(double rl_x2) {
        int rl = (int) rl_x2;
        rl = Math.max(Math.min(rl, 799), 1);
        return rl / 100 + 1;
    }

    public void makeAction(int x) {
        switch (x) {
            case 1:
                setTurnRight(enemyBearing + 90);
                setAhead(150);
                break;
            case 2:
                setTurnRight(enemyBearing + 90);
                setAhead(-150);
                break;
            case 3:
                turnGunRight(gunTurnTowardEnemyNeeded);
                turnRight(enemyBearing - 25);
                ahead(150);
                break;
            case 4:
                turnGunRight(gunTurnTowardEnemyNeeded);
                turnRight(enemyBearing - 25);
                back(150);
                break;
        }
    }


    public void onWin(WinEvent e) {
        logRoundStats(1);
    }

    public void onDeath(DeathEvent e) {
        logRoundStats(0);
    }

    private void logRoundStats(int win) {
//        int totalShots = hits + misses;
//        double accuracy = (totalShots > 0) ? ((double) hits / totalShots) : 0.0;

        try {
            File logDataFile = getDataFile(logFile);
            File headerFile = getDataFile(headerWrittenFile);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logDataFile, true))) {
                if (!headerFile.exists()) {
                    writer.println("reward,damage,damageTaken,win");
                    try {
                        if (headerFile.createNewFile()) {
                            out.println("Header file created.");
                        }
                    } catch (IOException ex) {
                        out.println("Failed to create header marker file: " + ex.getMessage());
                    }
                }
                writer.printf("%.2f,%.2f,%.2f,%d%n", cum_reward_while, totalDamage, totalDamageTaken, win);
            }
        } catch (IOException e) {
            out.println("Failed to write log: " + e.getMessage());
        }
    }


    public static int randInt(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    public static int maxIndex(double[] array) {
        double largest = array[0];
        int index = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] >= largest) {
                largest = array[i];
                index = i;
            }
        }
        return index;
    }


    /*
    Knowledge table management
     */

    public void initialiseLUT() {
        out.println("Initializing table");
        int[] total_states_actions = new int[8 * 6 * 4 * 4 * action.length];
        LUT = new String[total_states_actions.length][2];
        int z = 0;
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 6; j++) {
                for (int k = 1; k <= 4; k++) {
                    for (int l = 1; l <= 4; l++) {
                        for (int m = 1; m <= action.length; m++) {
                            LUT[z][0] = i + "" + j + "" + k + "" + l + "" + m;
                            LUT[z][1] = "0";
                            z = z + 1;
                        }
                    }
                }
            }
        }
    }

    public void saveLookUpTable() {
        out.println("Saving table");
        PrintStream w = null;
        try {
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile("LookUpTableX.txt")));
            for (int i = 0; i < LUT.length; i++) {
                w.println(LUT[i][0] + "    " + LUT[i][1]);
                if (i % 100 == 0)
                    out.println(LUT[i][0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            w.flush();
            w.close();
        }
    }

    public void saveCumulative() {
        PrintStream w = null;
        try {
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile("cum.txt")));
            for (int i = 0; i < cum_reward_array.length; i++) {
                w.println(cum_reward_array[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            w.flush();
            w.close();
        }
    }

    public void loadLookUpTable() throws IOException {
        out.println("Loading table");
        BufferedReader reader = new BufferedReader(new FileReader(getDataFile("LookUpTableX.txt")));
        String line = reader.readLine();
        try {
            int zz = 0;
            while (line != null) {
                String splitLine[] = line.split("    ");
                LUT[zz][0] = splitLine[0];
                LUT[zz][1] = splitLine[1];
                zz = zz + 1;
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
    }
}
