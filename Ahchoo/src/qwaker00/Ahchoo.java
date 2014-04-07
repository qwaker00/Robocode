package qwaker00;

import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.util.*;

public class Ahchoo extends AdvancedRobot
{
    static final double WALL_MARGIN = 160;
    static final double RAND_CHANGE_DIR = 5;
    static final double RAM_INDEX_MAX = 20;
    static final double FAR_DIST = 350;
    static final double CLOSE_DIST = 300;
    static Random R = new Random();
    static Enemy E = new Enemy();

    
    public void run() {
        Init();
        while (true) {
            RadarFolower();
            BodyMover();
            GunShooter();
            EndTurn();  
        }
    }

    void Init() {
        setColors(Color.black, Color.gray, Color.gray, Color.yellow, Color.white);
        E.Init();
        E.me = this;
//        ReverseStrategy = true;
        Waves = new ArrayList<Wave>();
        EWaves = new ArrayList<Wave>();
        shotMask = 0;
        revId = 0;
        if (gfStats == null) {
            gfStats = new GFHolder[10][3][3];
            for (int i = 0; i < 10; ++i) for (int j = 0; j < 3; ++j) for (int k = 0; k < 3; ++k)
                gfStats[i][j][k] = new GFHolder();
        }
        stopudov = new ArrayList<Bullet>();
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
    }

    void EndTurn() {
        E.EndTurn();
        execute();
    }


    
    /*
     *  I'm watching you
     */

    int scanDirection = 1;

    void RadarFolower() {
        if(!E.seeNow) {
            setTurnRadarRight(45 * scanDirection);
        } else {
            double diffAngle = Utils.normalRelativeAngle(getHeadingRadians()
                                                         - getRadarHeadingRadians()
                                                         + E.bearing
                                                        );
            if (diffAngle < 0) scanDirection = -1; else scanDirection = 1;
            setTurnRadarRightRadians(diffAngle);
        }
    }
    

    int moveDirection = 1, ramIndex = 0, minI, minJ;
    double minDiff;
    double wallResistX, wallResistY, moveX, moveY;
    double minD;
    int shotMask = 0, revId = 0;
    ArrayList<Bullet> stopudov;

    static boolean ReverseStrategy = true;
    static int eShotTotal = 0, eHitTotal = 0;
    static int mShotTotal = 0, mHitTotal = 0;
    static int stopudovHit = 0, stopudovShot = 0;
                
    double calcResist(double dist){
        dist = Math.min(dist, WALL_MARGIN);
        return Math.exp( (WALL_MARGIN  - dist) / 20 ) - 1;
    }
    

    void WallResistEvent() {
        wallResistX = calcResist(getX()) - calcResist(getBattleFieldWidth() - getX());
        wallResistY = calcResist(getY()) - calcResist(getBattleFieldHeight() - getY());
    }
            
    void BodyMover() {
        while (!Waves.isEmpty() && Waves.get(0).Check(E.x, E.y, getTime())) {
            Waves.remove(0);
        }
        while (!EWaves.isEmpty() && EWaves.get(0).Check(getX(), getY(), getTime())) {
            EWaves.remove(0);
        }

        minD = Double.POSITIVE_INFINITY;
        minI = -1;
        minJ = -1;

        WallResistEvent();
                
        if(R.nextDouble() * 100 < RAND_CHANGE_DIR) moveDirection *= -1;
                            
        if (E.seeNow) {
            if (E.energy == 0) {
                ++ramIndex;
                setAhead(0);
                if (ramIndex >= RAM_INDEX_MAX) {
                    setTurnRightRadians(E.bearing);
                    if (Math.abs(E.bearing) < 1e-1) {               
                        setAhead(100);
                    }
                }
                return;
            } else {
                ramIndex = 0;
            }

            if (E.IsEnergyDrop()) {
                shotMask = (shotMask << 1) & 31;

                //String shotMaskStr = Integer.toBinaryString(shotMask);
                //while (shotMaskStr.length() < 6) shotMaskStr = "0" + shotMaskStr;
                //System.out.println("ShotMask = " + shotMaskStr);

                EWaves.add(new Wave(E.x, E.y, E.lastenergy - E.energy, getTime() - 1));

                ++eShotTotal;
                if (ReverseStrategy && Math.abs(getVelocity()) > 4) {
                    moveDirection *= -1;
                }
            }
            
            double absEnemyAngle = getHeadingRadians() + E.bearing;
            double resultAngle = absEnemyAngle;
                                    
            double realHeading = getHeadingRadians() + ((moveDirection < 0) ? Math.PI : 0);
            double relativeAng = Utils.normalRelativeAngle(realHeading - absEnemyAngle);
        
            if (relativeAng > Math.PI * 5 / 6 || relativeAng < - Math.PI * 5 / 6) {
                moveDirection *= -1;
                relativeAng = Utils.normalRelativeAngle(absEnemyAngle - realHeading - Math.PI);
            }
            
            boolean cw = relativeAng < 0;
            resultAngle += Math.PI / 2 * (cw ? -1 : 1);
            
            if (E.distance > FAR_DIST) {
                resultAngle += Math.PI / 6 * (cw ? 1 : -1);
            } 
            if (E.distance < CLOSE_DIST) {
                resultAngle += Math.PI / 6 * (cw ? -1 : 1);
            }
            
            moveX = Math.sin(resultAngle) * 10;
            moveY = Math.cos(resultAngle) * 10;
        } else {
            moveX = 0;
            moveY = 1;
        }
        
        
        for (int i = 0; i < Waves.size(); ++i) {
            for (int j = 0; j < EWaves.size(); ++j) {
                double x1 = EWaves.get(j).startX;
                double y1 = EWaves.get(j).startY;
                
                double A = Waves.get(i).calcY(getTime() + 1) - y1;
                double B = x1 - Waves.get(i).calcX(getTime() + 1);
                double C = -A * x1 - B * y1;

                double D1 = Math.abs(A * getX() + B * getY() + C) / Math.sqrt(A * A + B * B);

                A = Waves.get(i).calcY(getTime() + 4) - y1;
                B = x1 - Waves.get(i).calcX(getTime() + 4);
                C = -A * x1 - B * y1;
                
                double D2 = Math.abs(A * getX() + B * getY() + C) / Math.sqrt(A * A + B * B);

                double D = Math.max(D1, D2);
                if (D > minD) continue;
                minD = D;
                minI = i;
                minJ = j;
            }
        }        
        if (minD <= 60) {
            double x1 = EWaves.get(minJ).startX;
            double y1 = EWaves.get(minJ).startY;
            double A = Waves.get(minI).calcY(getTime() + 4) - y1;
            double B = x1 - Waves.get(minI).calcX(getTime() + 4);
            double C = -A * x1 - B * y1;
            double D1 = (A * getX() + B * getY() + C) / Math.sqrt(A * A + B * B);

            A = Waves.get(minI).calcY(getTime() + 2) - y1;
            B = x1 - Waves.get(minI).calcX(getTime() + 2);
            C = -A * x1 - B * y1;
            double D = (A * getX() + B * getY() + C) / Math.sqrt(A * A + B * B);

            if (Math.abs(D1 - D) > 10) {
                moveX = - A * D / Math.sqrt(A * A + B * B);
                moveY = - B * D / Math.sqrt(A * A + B * B);
                minDiff = D1 - D;
            }
        }


        moveX += wallResistX;
        moveY += wallResistY;        
        double diffAngle = Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(moveY, moveX) + ((moveDirection < 0) ? Math.PI : 0)  - getHeadingRadians());

        if (diffAngle > Math.PI * 5 / 6 || diffAngle < - Math.PI * 5 / 6) {
            moveDirection *= -1;
            diffAngle = Utils.normalRelativeAngle(diffAngle + Math.PI);
        }

        if (ReverseStrategy) {
            setColors(Color.black, Color.gray, Color.gray, Color.yellow, Color.white);
        } else {
            setColors(Color.white, Color.gray, Color.gray, Color.yellow, Color.white);
        }
        
        setTurnRightRadians(diffAngle);     

        setAhead(100 * moveDirection);
    }
    

    ArrayList<Wave> Waves = new ArrayList<Wave>();
    ArrayList<Wave> EWaves = new ArrayList<Wave>();
    static GFHolder[][][] gfStats = null;
    static int direction = 1;

    void GunShooter() {
        if (!E.seeNow || ramIndex > 0) return;          

        double bPower = 600 / E.distance;
        bPower = Math.min(getEnergy() / 3, Math.max(0.1, Math.min(2, bPower)));

        if (E.velocity != 0) {
            if (Math.sin(E.heading - getHeadingRadians() - E.bearing) * E.velocity < 0)
                direction = -1;
            else
                direction = 1;
        }



        int dFactor = (int)Math.floor(E.distance / 200);
        int xFactor = (E.x < getBattleFieldWidth() / 6.) ? 0 : ((E.x > getBattleFieldWidth() * 5. / 6.) ? 2 : 1);
        int yFactor = (E.y < getBattleFieldHeight() / 6.) ? 0 : ((E.y > getBattleFieldHeight() * 5. / 6.) ? 2 : 1);
        GFHolder gfStat = gfStats[dFactor][xFactor][yFactor];
        int gfMax = gfStat.Predict();
        double maxAngle = Math.asin(8. / Rules.getBulletSpeed(bPower));
        double angle = getHeadingRadians() + E.bearing + direction * ((double)(gfMax - gfStat.length() / 2) * maxAngle / (gfStat.length() / 2));
        double diffAngle = Utils.normalRelativeAngle(angle - getGunHeadingRadians());

        setTurnGunRightRadians(diffAngle);

        if (E.distance <= 70) {
            angle = getHeadingRadians() + E.bearing;
            diffAngle = Utils.normalRelativeAngle(angle - getGunHeadingRadians());
            setTurnGunRightRadians(diffAngle);
            if (getGunHeat() == 0 && Math.abs(diffAngle) < 1e-2) {
                Bullet b = setFireBullet(3.0);
                if (b != null) {
                    stopudov.add( b );
                    ++stopudovShot;
                    ++mShotTotal;
                    Waves.add(new Wave(getX(), getY(), bPower, getHeadingRadians() + E.bearing, maxAngle, direction, gfStat, getTime(), getGunHeadingRadians()));
                }
            }
        } else
        if (bPower >= 0.1 && E.distance <= 450 && getGunHeat() == 0 && Math.abs(diffAngle) < Math.atan2(8, E.distance)) {
            Bullet b = setFireBullet(bPower);
            if (b != null) {
                ++mShotTotal;
                Waves.add(new Wave(getX(), getY(), bPower, getHeadingRadians() + E.bearing, maxAngle, direction, gfStat, getTime(), getGunHeadingRadians()));
            }
        }
    }

    public void onPaint(Graphics2D g) {
        g.setColor(Color.green);
        g.drawString(E.distance + "", 0, 0);

        g.setColor(new Color(200, 0, 0, 80));
        g.drawLine(0, (int)(getBattleFieldHeight() / 6), 800, (int)(getBattleFieldHeight() / 6));
        g.drawLine(0, (int)(getBattleFieldHeight() * 5 / 6), 800, (int)(getBattleFieldHeight() * 5 / 6));
        g.drawLine((int)(getBattleFieldWidth() / 6), 0, (int)(getBattleFieldWidth() / 6), 600);
        g.drawLine((int)(getBattleFieldWidth() * 5 / 6), 0, (int)(getBattleFieldWidth() * 5 / 6), 600);

        
        g.setColor(new Color(0, 0, 0, 30));
        for (int i = 1; i <= 7; i++) {
            g.drawOval((int)getX() - i * 200, (int)getY() - i*200, i*400, i*400);
        }
        g.setColor(Color.red);
        g.fillOval((int)E.x - 3, (int)E.y - 3, 6, 6);

        for (Wave Wave : Waves) {
            Wave.Paint(g, getTime() + 1);
        }

        for (Wave EWave : EWaves) {
            EWave.Paint(g, getTime() + 1);
        }

        g.setColor(Color.pink);
        g.drawLine((int)getX() + (int)moveX * 10, (int)getY() + (int)moveY * 10, (int)getX(), (int)getY());
        g.drawString(moveX + " " + moveY, 0, 60);

        if (minI != -1 && minD <= 60) {
            
            g.setColor(Color.blue);
            int x1 = (int)EWaves.get(minJ).startX;
            int y1 = (int)EWaves.get(minJ).startY;
                                                                                 
            int x2 = (int)(EWaves.get(minJ).startX + 10 * (Waves.get(minI).calcX(getTime()) - x1));
            int y2 = (int)(EWaves.get(minJ).startY + 10 * (Waves.get(minI).calcY(getTime()) - y1));
            g.drawLine(x1, y1, x2, y2);

            x2 = (int)(EWaves.get(minJ).startX + 10 * (Waves.get(minI).calcX(getTime() + 3) - x1));
            y2 = (int)(EWaves.get(minJ).startY + 10 * (Waves.get(minI).calcY(getTime() + 3) - y1));
            g.drawLine(x1, y1, x2, y2);
            //x2 = (int)(EWaves.get(minJ).startX + 10 * (Waves.get(minI).calcX(getTime() + 4) - x1));
            //y2 = (int)(EWaves.get(minJ).startY + 10 * (Waves.get(minI).calcY(getTime() + 4) - y1));
            //g.drawLine(x1, y1, x2, y2);

            g.drawString(minD + "", 0, 20);
            g.drawString(minDiff + "", 0, 40);
        }

    }

    public void onScannedRobot(ScannedRobotEvent e) {
        E.Log(e);
    }

    public void onBattleEnded(BattleEndedEvent e) {
        System.out.println("Enemy stat: " + eHitTotal + "/" + eShotTotal + " " + (eHitTotal*100/eShotTotal) + "%");
        System.out.println("My stat   : " + mHitTotal + "/" + mShotTotal + " " + (mHitTotal*100/mShotTotal) + "%");
        if (stopudovShot > 0)
            System.out.println("Close shot: " + stopudovHit + "/" + stopudovShot + " " + (stopudovHit*100/stopudovShot) + "%");
    }

    public void onRobotDeath(RobotDeathEvent e){
    }

    public void onDeath(DeathEvent event) {
    }
    
    public void onHitRobot(HitRobotEvent e){
        if (getVelocity() == 0) moveDirection *= -1;
    }

    public void onHitWall(HitWallEvent e){
        if (getVelocity() == 0) moveDirection *= -1;
    }

    public void onHitByBullet(HitByBulletEvent e){
        shotMask |= 1;

        //String shotMaskStr = Integer.toBinaryString(shotMask);
        //while (shotMaskStr.length() < 6) shotMaskStr = "0" + shotMaskStr;
        //System.out.println("ShotMask = " + shotMaskStr);

        if (Integer.bitCount(shotMask) > 2) {           
            System.out.println("Turn strategy");

            ReverseStrategy ^= true;
            shotMask = 0;
        }

        ++eHitTotal;
        E.IHit();
    }

    public void onBulletHit(BulletHitEvent e) {
        Bullet b = e.getBullet();
        for (Bullet aStopudov : stopudov) {
            if (b.equals(aStopudov)) {
                ++stopudovHit;
                break;
            }
        }

        E.Hit();
        ++mHitTotal;
    }
}
                                                                                                                                                                                                                                                                                                                                        