package forkbot; //change it into your package name

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.*;

import robocode.*;

import java.util.Random;

public class ForkBot extends AdvancedRobot {
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
    int qenemy_x = 0;
    int qenemy_y = 0;
    private RobotStatus robotStatus;
    int qdistancetoenemy = 0;

    double absbearing = 0;
    int q_absbearing = 0;

    //initialize reward
    double reward = 0;
    String state_action_combi = null;
    String state_action_combi_greedy = null;
    double robot_energy = 0;
    int sa_combi_inLUT = 0;

    //Run command-Robocode
    String q_present = null;
    double q_present_double = 0;
    int random_action = 0;
    String state_action_combi_next = null;
    int sa_combi_inLUT_next = 0;
    String q_next = null;
    double q_next_double = 0;
    int count = 0;
    int Qmax_action = 0;
    int[] actions_indices = new int[total_actions.length];
    double[] q_possible = new double[total_actions.length];
    int Qmax_actual_action = 0;
    double enemy_energy = 0;
    double reward1 = 0;
    double my_energy_pres = 0;
    double enemy_energy_pres = 0;
    double my_energy_next = 0;
    double enemy_energy_next = 0;
    double gunTurnAmt;

    private double getHeadingRadians;
    private double getVelocity;
    private double absBearing;
    private double getBearing;
    private double getTime;
    private double normalizeBearing;

    double cum_reward_while = 0;
    static double[] cum_reward_array = new double[1000];
    static int index1 = 0;

    // Fields to track stats
//    private double totalReward = 0;
    private double totalDamage = 0;
    private static String logFile;
    private static String headerWrittenFile;

    private static final String TYPE_NAME = "orgV1";
    private final String PARAMETERS = String.format("A%.2f_G%.2f_E%.2f",
            alpha, gamma, 0.5);


    //-------------Explore or greedy----------------------//
    boolean explore = true;
    boolean greedy = true;
    //----------------------------------------------------//

    public void run() {
        logFile = "unprocessed_" + TYPE_NAME + "_" + PARAMETERS + ".log";
        headerWrittenFile = logFile + ".header_written.tmp";

        if (count == 0) {
            //For initializing text file in the first run use the three lines of code. once the text file is generated in \Rl_check comment this out
            initialiseLUT();
            saveLookUpTable();
            //comment this
            try {
                loadLookUpTable();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        count += 1;

        setColors(null, new Color(192, 192, 192), new Color(192, 192, 192), Color.black, new Color(150, 0, 150));
        setBodyColor(new java.awt.Color(192, 192, 192, 100));


        //noinspection InfiniteLoopStatement
        while (true) {
            if (explore) { //Explore event--------------------------------------------------//
                saveLookUpTable();
                //load command
                try {
                    loadLookUpTable();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //load command
                //predict current state:
                turnGunRight(360);
                random_action = randInt(1, total_actions.length);
                state_action_combi = qrl_x + "" + qrl_y + "" + qdistancetoenemy + "" + q_absbearing + "" + random_action;

                for (int i = 0; i < LUT.length; i++) {
                    if (LUT[i][0].equals(state_action_combi)) {
                        sa_combi_inLUT = i;
                        break;
                    }
                }
                q_present = LUT[sa_combi_inLUT][1];
                q_present_double = Double.parseDouble(q_present);
                reward = 0;

                //performing next state and scanning
                my_energy_pres = robot_energy;
                enemy_energy_pres = enemy_energy;
                makeAction(random_action);

                turnGunRight(360);
                my_energy_next = robot_energy;
                enemy_energy_next = enemy_energy;

                reward1 = (my_energy_next - my_energy_pres) - (enemy_energy_next - enemy_energy_pres);

                state_action_combi_next = qrl_x + "" + qrl_y + "" + qdistancetoenemy + "" + q_absbearing + "" + random_action;
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

            }//explore loop ends

//Greedy Moves//

            if (greedy) {
                saveLookUpTable();
                //load command
                try {
                    loadLookUpTable();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //load command
                //predict current state:
                turnGunRight(360);
                // finding action that produces maximum Q value

                for (int j = 1; j <= total_actions.length; j++) {
                    state_action_combi = qrl_x + "" + qrl_y + "" + qdistancetoenemy + "" + q_absbearing + "" + j;

                    for (int i = 0; i < LUT.length; i++) {
                        if (LUT[i][0].equals(state_action_combi)) {
                            actions_indices[j - 1] = i;
                            break;

                        }
                    }

                }
                //converting table to double
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
                int jj = 0;

                //find position of actions
                for (int i = 0; i < 4; i++) {
                    if (actions_indices[i] == Qmax_action) {
                        Qmax_actual_action = i + 1;
                    }
                }
                //find position of actions

                //finding action that produces maximum q
                state_action_combi_greedy = qrl_x + "" + qrl_y + "" + qdistancetoenemy + "" + q_absbearing + "" + Qmax_action;

                for (int i = 0; i < LUT.length; i++) {
                    if (LUT[i][0].equals(state_action_combi_greedy)) {
                        sa_combi_inLUT = i;
                        break;
                    }
                }


                q_present = LUT[sa_combi_inLUT][1];
                q_present_double = Double.parseDouble(q_present);
                reward = 0;

                //performing next state and scanning
                reward1 = 0;
                my_energy_pres = robot_energy;
                enemy_energy_pres = enemy_energy;

                makeAction(Qmax_action);


                turnGunRight(360);

                my_energy_next = robot_energy;
                enemy_energy_next = enemy_energy;
                reward1 = (my_energy_next - my_energy_pres) - (enemy_energy_next - enemy_energy_pres);

                state_action_combi_next = qrl_x + "" + qrl_y + "" + qdistancetoenemy + "" + q_absbearing + "" + Qmax_action;
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

            }//greedy loop ends


        }//while loop ends
        //System.out.println(cum_reward_while);


    }//run function ends


    //function definitions for RL robot:
    public void onScannedRobot(ScannedRobotEvent e) {
        double absBearing = e.getBearingRadians() + getHeadingRadians();
        this.absBearing = absBearing;
        double getVelocity = e.getVelocity();
        double getHeadingRadians = e.getHeadingRadians();
        this.getHeadingRadians = getHeadingRadians;
        this.getVelocity = getVelocity;

        double getBearing = e.getBearing();
        this.getBearing = getBearing;
        double getTime = getTime();
        this.getTime = getTime;
        gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
        this.gunTurnAmt = gunTurnAmt;

        double normalizeBearing = normalizeBearing(getBearing + 90 - (15 * 1));
        this.normalizeBearing = normalizeBearing;
        robot_energy = getEnergy();
        enemy_energy = e.getEnergy();
        distance = e.getDistance(); //distance to the enemy
        qdistancetoenemy = quantize_distance(distance); //distance to enemy state number 3


        if (qdistancetoenemy == 1) {
            fire(3);
        }
        if (qdistancetoenemy == 2) {
            fire(2);
        }
        if (qdistancetoenemy == 3) {
            fire(1);
        }


        qrl_x = quantize_position(getX()); //your x position -state number 1
        qrl_y = quantize_position(getY()); //your y position -state number 2
        //Calculating Enemy X & Y:
        double angleToEnemy = e.getBearing();
        // Calculate the angle to the scanned robot
        double angle = Math.toRadians((getHeading() + angleToEnemy % 360));
        // Calculate the coordinates of the robot
        double enemyX = (getX() + Math.sin(angle) * e.getDistance());
        double enemyY = (getY() + Math.cos(angle) * e.getDistance());
        qenemy_x = quantize_position(enemyX); //enemy x-position
        qenemy_y = quantize_position(enemyY); //enemy y-position

        //distance to enemy
        //absolute angle to enemy
        absbearing = absoluteBearing((float) getX(), (float) getY(), (float) enemyX, (float) enemyY);

        q_absbearing = quantize_angle(absbearing); //state number 4

    }

    public double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;

    }


//reward functions:


    public void onHitRobot(HitRobotEvent event) {
        reward -= 2;
    } //our robot hit by enemy robot

    public void onBulletHit(BulletHitEvent event) {
        reward += 3;
        totalDamage += event.getBullet().getPower() * 4;
    } //one of our bullet hits enemy robot

    public void onHitByBullet(HitByBulletEvent event) {
        reward -= 3;
    } //when our robot is hit by a bullet
//public void BulletMissedEvent(Bullet bullet){reward-=3;}

    private int quantize_angle(double absbearing2) {

        if ((absbearing2 > 0) && (absbearing2 <= 90)) {
            q_absbearing = 1;
        } else if ((absbearing2 > 90) && (absbearing2 <= 180)) {
            q_absbearing = 2;
        } else if ((absbearing2 > 180) && (absbearing2 <= 270)) {
            q_absbearing = 3;
        } else if ((absbearing2 > 270) && (absbearing2 <= 360)) {
            q_absbearing = 4;
        }
        return q_absbearing;
    }

    private int quantize_distance(double distance2) {

        if ((distance2 > 0) && (distance2 <= 250)) {
            qdistancetoenemy = 1;
        } else if ((distance2 > 250) && (distance2 <= 500)) {
            qdistancetoenemy = 2;
        } else if ((distance2 > 500) && (distance2 <= 750)) {
            qdistancetoenemy = 3;
        } else if ((distance2 > 750) && (distance2 <= 1000)) {
            qdistancetoenemy = 4;
        }

        return qdistancetoenemy;
    }

    //absolute bearing
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
        rl = Math.max(Math.min(rl, 800), 0);

        if ((rl_x2 > 0) && (rl_x2 <= 100)) {
            qrl_x = 1;
        } else if ((rl_x2 > 100) && (rl_x2 <= 200)) {
            qrl_x = 2;
        } else if ((rl_x2 > 200) && (rl_x2 <= 300)) {
            qrl_x = 3;
        } else if ((rl_x2 > 300) && (rl_x2 <= 400)) {
            qrl_x = 4;
        } else if ((rl_x2 > 400) && (rl_x2 <= 500)) {
            qrl_x = 5;
        } else if ((rl_x2 > 500) && (rl_x2 <= 600)) {
            qrl_x = 6;
        } else if ((rl_x2 > 600) && (rl_x2 <= 700)) {
            qrl_x = 7;
        } else if ((rl_x2 > 700) && (rl_x2 <= 800)) {
            qrl_x = 8;
        }
        return qrl_x;
    }

    public void makeAction(int x) {
        switch (x) {
            case 1:
                setTurnRight(getBearing + 90);
                setAhead(150);
                break;
            case 2:
                setTurnRight(getBearing + 90);
                setAhead(-150);
                break;
            case 3:
                turnGunRight(gunTurnAmt);
                turnRight(getBearing - 25);
                ahead(150);
                break;
            case 4:
                turnGunRight(gunTurnAmt);
                turnRight(getBearing - 25);
                back(150);
                break;
        }
    }

    public void onRoundEnded(RoundEndedEvent e) {
        System.out.println("cumulative reward of one full battle is ");
        System.out.println(cum_reward_while);
        System.out.println("index number ");
        System.out.println(getRoundNum());
        cum_reward_array[getRoundNum()] = cum_reward_while;

        for (int i = 0; i < cum_reward_array.length; i++) {
            System.out.println(cum_reward_array[i]);
            System.out.println();
        }

        index1 = index1 + 1;
        saveCumulative();
    }

    public void onBattleEnded(BattleEndedEvent e) {
        saveCumulative();
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
                    writer.println("reward,damage,accuracy,win");
                    try {
                        if (headerFile.createNewFile()) {
                            out.println("Header file created.");
                        }
                    } catch (IOException ex) {
                        out.println("Failed to create header marker file: " + ex.getMessage());
                    }
                }
                writer.printf("%.2f,%.2f,%.4f,%d%n", cum_reward_while, totalDamage, 0.0, win);
            }
        } catch (IOException e) {
            out.println("Failed to write log: " + e.getMessage());
        }
    }


    public static int randInt(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    public void initialiseLUT() {
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
        PrintStream w = null;
        try {
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile("LookUpTable.txt")));
            for (int i = 0; i < LUT.length; i++) {
                w.println(LUT[i][0] + "    " + LUT[i][1]);
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
        BufferedReader reader = new BufferedReader(new FileReader(getDataFile("LookUpTable.txt")));
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
}
