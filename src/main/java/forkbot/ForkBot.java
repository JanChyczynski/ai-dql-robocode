package forkbot; //change it into your package name

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

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
    double gunTurnTowardEnemyNeeded;

    private double enemyHeadingRadians;
    private double enemyVelocity;
    private double enemyAbsBearingRadians;
    private double enemyBearing;
    private double time;
    private double normalizedBearing;

    double cum_reward_while = 0;
    static double[] cum_reward_array = new double[1000];
    static int index1 = 0;


    //-------------Explore or greedy----------------------//
    boolean explore = true;
    boolean greedy = true;
    //----------------------------------------------------//

    public void run() {
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
                state_action_combi = "" + qrl_x + qrl_y + qdistancetoenemy + q_absbearing + random_action;

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

                state_action_combi_next = "" + qrl_x + qrl_y + qdistancetoenemy + q_absbearing + random_action;
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
                    state_action_combi = "" + qrl_x + qrl_y + qdistancetoenemy + q_absbearing + j;

                    // kurde nawet hashmapy nie umią
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
                state_action_combi_greedy = "" + qrl_x + qrl_y + qdistancetoenemy + q_absbearing + Qmax_action;

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
                my_energy_pres = robot_energy;
                enemy_energy_pres = enemy_energy;

                makeAction(Qmax_action);
                turnGunRight(360);

                my_energy_next = robot_energy;
                enemy_energy_next = enemy_energy;
                reward1 = (my_energy_next - my_energy_pres) - (enemy_energy_next - enemy_energy_pres);

                state_action_combi_next = "" + qrl_x + qrl_y + qdistancetoenemy + q_absbearing + Qmax_action;
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
        enemy_energy = e.getEnergy();
        distance = e.getDistance();
        qdistancetoenemy = quantize_distance(distance);

        if (qdistancetoenemy == 1) {
            fire(3);
        }
        if (qdistancetoenemy == 2) {
            fire(2);
        }
        if (qdistancetoenemy == 3) {
            fire(1);
        }

        qrl_x = quantize_position(getX());
        qrl_y = quantize_position(getY());

        //Calculating Enemy X & Y:
        double angleToEnemy = e.getBearing();
        double angle = Math.toRadians((getHeading() + angleToEnemy % 360));
        double enemyX = (getX() + Math.sin(angle) * e.getDistance());
        double enemyY = (getY() + Math.cos(angle) * e.getDistance());
        qenemy_x = quantize_position(enemyX); //enemy x-position
        qenemy_y = quantize_position(enemyY); //enemy y-position

        //absolute angle to enemy
        absbearing = absoluteBearing((float) getX(), (float) getY(), (float) enemyX, (float) enemyY);
        q_absbearing = quantize_angle(absbearing);
    }


    /*
    Events
     */

    public void onHitRobot(HitRobotEvent event) {
        reward -= 2;
    }

    public void onBulletHit(BulletHitEvent event) {
        reward += 3;
    }

    public void onHitByBullet(HitByBulletEvent event) {
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

    private int quantize_distance(double distance2) {
        int d = (int) Math.max(Math.min(distance2, 1000), 0);
        qdistancetoenemy = d / 250 + 1;
        return qdistancetoenemy;
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
}
