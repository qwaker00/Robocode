package qwaker00;

import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.util.*;

/*
 *  All you need is love
 */

public class Ahchoo extends AdvancedRobot
{
	static final double WALL_MARGIN = 130;
	static final double RAND_CHANGE_DIR = 2;
	static final double RAM_INDEX_MAX = 30;
	static final double FAR_DIST = 150;
	static final double CLOSE_DIST = 100;
	static Random R = new Random();
	static Enemy E = new Enemy();

	
	/*
	 * Run, Gandhi, run!
	 */	
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
		if (gfStats == null) {
			gfStats = new GFHolder[10][3][3];
			for (int i = 0; i < 10; ++i) for (int j = 0; j < 3; ++j) for (int k = 0; k < 3; ++k)
				gfStats[i][j][k] = new GFHolder();
		}
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
	

	
	/*
	 *  Shake your body, buddy
	 */

	int moveDirection = 1, ramIndex = 0;
	double wallResistX, wallResistY, moveX, moveY;
	int enemyShot = 0, enemyHits = 0;
	static boolean ReverseStrategy = true;
				
	double calcResist(double dist){
		dist = Math.min(dist, WALL_MARGIN);
		return Math.exp( (WALL_MARGIN  - dist) / 20 ) - 1;
	}
	
	void WallResistEvent() {
		wallResistX = calcResist(getX()) - calcResist(getBattleFieldWidth() - getX());
		wallResistY = calcResist(getY()) - calcResist(getBattleFieldHeight() - getY());
	}
			
	void BodyMover() {
		WallResistEvent();
				
		if(R.nextDouble() * 100 < RAND_CHANGE_DIR) moveDirection *= -1;
							
		if (E.seeNow) {
			if (E.energy == 0) {
				++ramIndex;
				setAhead(0);
				if (ramIndex >= RAM_INDEX_MAX) {
					setTurnRightRadians(E.bearing);
					if (Math.abs(E.bearing) < 1e-2) {				
						setAhead(100);
					}
				}
				return;
			} else {
				ramIndex = 0;
			}

			if (E.IsEnergyDrop()) {
				++enemyShot;
				if (enemyHits > 0.3 * enemyShot && enemyShot > 10) {
					ReverseStrategy ^= true;
				}
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
			moveY = 10;
		}
		moveX += wallResistX;
		moveY += wallResistY;
		
		double diffAngle = Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(moveY, moveX) + ((moveDirection < 0) ? Math.PI : 0)  - getHeadingRadians());

		if (diffAngle > Math.PI * 5 / 6 || diffAngle < - Math.PI * 5 / 6) {
			moveDirection *= -1;
			diffAngle = Utils.normalRelativeAngle(diffAngle + Math.PI);
		}

		setTurnRightRadians(diffAngle);		
		setAhead(100 * moveDirection);
	}
	

	/*
	 *  1 shot = 44.3602943 milliliters
	 *	                           google.com	
	 */

	double predX, predY;
	ArrayList<Wave> Waves = new ArrayList<Wave>();
	static GFHolder[][][] gfStats = null;
	static int gfStatLength = 0;
	static int direction = 1;

	void GunShooter() {
		if (!E.seeNow || ramIndex > 0) return;			
		double bPower = 550 / E.distance;
		bPower = Math.min(getEnergy() / 3, Math.max(0.1, Math.min(3, bPower)));

		while (!Waves.isEmpty() && Waves.get(0).Check(E.x, E.y, getTime())) {
			Waves.remove(0);
		}

		if (E.velocity != 0) {
			if (Math.sin(E.heading - getHeadingRadians() - E.bearing) * E.velocity < 0)
				direction = -1;
			else
				direction = 1;
		}


//		int dFactor = (int)Math.floor(E.distance / 100);
//		int xFactor = (E.x < getBattleFieldWidth() / 6.) ? 0 : ((E.x > getBattleFieldWidth() * 5. / 6.) ? 2 : 1);
//		int yFactor = (E.y < getBattleFieldHeight() / 6.) ? 0 : ((E.y > getBattleFieldHeight() * 5. / 6.) ? 2 : 1);
		int dFactor = 0;
		int xFactor = 0, yFactor = 0;

		GFHolder gfStat = gfStats[dFactor][xFactor][yFactor];
		double maxAngle = Math.asin(8. / Rules.getBulletSpeed(bPower));
		int gfMax = gfStat.Predict();
		double angle = getHeadingRadians() + E.bearing + direction * ((double)(gfMax - gfStat.length() / 2) * maxAngle / (gfStat.length() / 2));
//		System.out.println(E.bearing + " " + E.heading + " " + angle);
		double diffAngle = Utils.normalRelativeAngle(angle - getGunHeadingRadians());
        setTurnGunRightRadians(diffAngle);
		if (bPower >= 0.1 && E.distance <= 300 && getGunHeat() == 0 && Math.abs(diffAngle) < Math.atan2(8, E.distance)) {
			setFire(bPower);
			Waves.add(new Wave(getX(), getY(), bPower, getHeadingRadians() + E.bearing, maxAngle, direction, gfStat, getTime()));
//			System.out.println(getTime());
		}
	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(200, 0, 0, 80));
		g.drawLine(0, (int)(getBattleFieldHeight() / 6), 800, (int)(getBattleFieldHeight() / 6));
		g.drawLine(0, (int)(getBattleFieldHeight() * 5 / 6), 800, (int)(getBattleFieldHeight() * 5 / 6));
		g.drawLine((int)(getBattleFieldWidth() / 6), 0, (int)(getBattleFieldWidth() / 6), 600);
		g.drawLine((int)(getBattleFieldWidth() * 5 / 6), 0, (int)(getBattleFieldWidth() * 5 / 6), 600);

		
		g.setColor(new Color(0, 0, 0, 30));
		for (int i = 1; i <= 7; i++) {
			g.drawOval((int)getX() - i * 100, (int)getY() - i*100, (int)i*200, (int)i*200);
		}
		g.setColor(Color.red);
		g.fillOval((int)E.x - 3, (int)E.y - 3, 6, 6);

		for (int i = 0; i < Waves.size(); ++i) {
			Waves.get(i).Paint(g, getTime());
		}
	}

	public void onBulletHit(BulletHitEvent event) {
   		gfStats[0][0][0].Decay();
    }

	public void onScannedRobot(ScannedRobotEvent e) {
		E.Log(e);
	}

	public void onRoundEnded(RoundEndedEvent e) {
		E.EndRound();
	}

	public void onRobotDeath(RobotDeathEvent e){
		E.Die();
	}

	public void onDeath(DeathEvent event) {
		E.IDie();
		ReverseStrategy ^= true;
	}
	
	public void onHitRobot(HitRobotEvent e){
		E.Hit();
		if (getVelocity() == 0) moveDirection *= -1;
	}

	public void onHitByBullet(HitByBulletEvent e){
		E.IHit();
	}
}

class GFHolder {
	double[] hits = new double[31];
	final double HISTORY = 250.;

	public int length() {
		return hits.length;
	}

	public void Hit(int gfIndex) {
		hits[gfIndex] = (hits[gfIndex] * HISTORY + 1.) / (HISTORY + 1);
		for (int i = 0; i < 31; ++i) if (i != gfIndex) {
			hits[i] = hits[i] * HISTORY / (HISTORY + 1);
		}
	}

	public void Decay() {
		for (int i = 0; i < 31; ++i) {
			hits[i] = hits[i] * HISTORY / (HISTORY * 5);
		}		
	}

	public int Predict() {
		int gfMax = hits.length / 2;

		double maxCost = -1;
		for (int i = 0; i < hits.length; ++i) {
			double cost = 0, d = Math.min(30 - i, Math.min(i, 3));

			if (d == 0) cost = hits[i];else
			for (int j = 0; j < hits.length; ++j) {
				cost += hits[j] * 1. / (d * 2 * Math.PI) * Math.exp(-(j - i)*(j - i) / (2 * d * d));
			}
			
			if (cost > maxCost) {
				gfMax = i;
				maxCost = cost;
			}
		}

		return gfMax;
	}
}

class Wave {
	AdvancedRobot owner;
	double bulletSpeed, heading, maxAngle, startX, startY;
	GFHolder result;
	int direction;
	long startTime;

	public Wave(double startX, double startY, double bulletPower, double heading, double maxAngle, int direction, GFHolder result, long startTime) {
		this.startX = startX;
		this.startY = startY;
		this.direction = direction;
		this.bulletSpeed = Rules.getBulletSpeed(bulletPower);
		this.startTime = startTime;
		this.heading = heading;
		this.maxAngle = maxAngle;
		this.result = result;
	}

	public boolean Check(double eX, double eY, long time) {
		if ( (Math.sqrt((eX - startX)*(eX - startX) + (eY - startY)*(eY - startY)) - 15) / bulletSpeed <= time - startTime) {
			double diffAngle = direction * Utils.normalRelativeAngle(Math.PI/2 - Math.atan2(eY - startY, eX - startX) - heading);
			int gfIndex = (int)((result.length() - 1) / 2 + Math.round((result.length() - 1) / 2 * Math.min(1., Math.max(-1., (diffAngle / maxAngle)))));
			result.Hit(gfIndex);
//			System.out.println(diffAngle + " " + maxAngle);
//			System.out.println("GF " + gfIndex + "++");
			return true;
		}
		return false;
	}

	void Paint(Graphics g, long time) {
		g.setColor(Color.white);
		int r = (int)Math.round(bulletSpeed * (time - startTime)) - 15;
		g.drawOval((int)startX - r, (int)startY - r, r + r, r + r);
		g.drawLine((int)startX, (int)startY, (int)(Math.sin(heading) * r + startX), (int)(Math.cos(heading) * r + startY));
		g.setColor(Color.green);
		g.drawLine((int)startX, (int)startY, (int)(Math.sin(heading + maxAngle * direction) * r + startX), (int)(Math.cos(heading + maxAngle * direction) * r + startY));
		g.setColor(Color.red);
		g.drawLine((int)startX, (int)startY, (int)(Math.sin(heading - maxAngle * direction) * r + startX), (int)(Math.cos(heading - maxAngle * direction) * r + startY));
	}
};


class Enemy {
/*	private int N_FACTORS = 5;
	private ArrayList<double[]> factorHistory = new ArrayList<double[]>(3000);
	private double[] coefFactor = new double[] {10., 10., 0, 0, 0};

	private void CalcFactors() {
		if (!history.isEmpty())  {
			double[] last = history.get(history.size() - 1);
			double[] factors = new double[]
			{
				last[2] * (x - last[0]) + last[3] * (y - last[1]), 
				last[2] * (y - last[1]) - last[3] * (x - last[0]),
				1000. / Math.min(Math.min(x, y), Math.min(me.getBattleFieldWidth() - x, me.getBattleFieldHeight() - y)),
				distance,
				velocity
			};

			System.out.print("Factors:");
			System.out.print(factors[0] + ", ");
			System.out.print(factors[1] + ", ");
			System.out.print(factors[2] + ", ");
			System.out.print(factors[3] + ", ");
			System.out.print(factors[4] + "\n");
			System.out.println();

			factorHistory.add(factors);
		}
	}
	
*/
	public AdvancedRobot me;
	public double distance, heading, bearing, velocity, energy, lastenergy, x, y;
	public boolean seeNow = false;
	public ArrayList<double[]> history = new ArrayList<double[]>(3000);
	public int ActualHistorySize;

	public void Hit() {
		energy -= 3;
	}

	public void EndRound() {
	}

	public void IDie() {
	}

	public void Die() {
	}

	public void IHit() {
		
	}
	
/*

	public double[][] Predict(int cnt, int learn, Graphics2D g) {
		double[][] result = null;

		if (ActualHistorySize <= learn + Math.max(learn, cnt))  {
			// Z - z - z
		} else {
			double[][] learnFactors = new double[learn][];
			for (int i = 0; i < learn; ++i) {
				learnFactors[i] = factorHistory.get(factorHistory.size() - learn + i);
			}

			double minCost = Double.MAX_VALUE;
			int minPosition = 0;

			for (int i = 0; i < factorHistory.size() - learn - Math.max(learn, cnt); ++i) {
				double cost = 0.;
				for (int j = 0; j < learn; ++j) {
					double diff = 0.;
					for (int k = 0; k < N_FACTORS; k++) {
						double localDiff = learnFactors[j][k] - factorHistory.get(i + j)[k];
						diff += localDiff * localDiff * coefFactor[k];
					}
					cost += diff;
				}
				if (cost <= minCost) {
					minPosition = i;
					minCost = cost;
				}
			}

			DecimalFormat df = new DecimalFormat("#.###");
			DecimalFormatSymbols custom = new DecimalFormatSymbols();
			custom.setDecimalSeparator('.');
			df.setDecimalFormatSymbols(custom);

			result = new double[cnt][2];

			double[] prev = history.get(minPosition + learn);
			double pvx = 0, pvy = 0;
			for (int i = learn - 1; i >= 0; --i) {
				double x = history.get(minPosition + i)[2], y = history.get(minPosition + i)[3];
				if (x*x+y*y > 1e-6) {
					pvx = x;
					pvy = y;
					break;
				}
			}

			double[] last = history.get(history.size() - 1);
			double lvx = 0, lvy = 0;
			for (int i = learn - 1; i >= 0; --i) {
				double x = history.get(history.size() - learn + i)[2], y = history.get(history.size() - learn + i)[3];
				if (x*x+y*y > 1e-6) {
					lvx = x;
					lvy = y;
					break;
				}
			}

			double diffAng = - Math.atan2(lvx, lvy) + Math.atan2(pvx, pvy);
//			System.out.println(diffAng);
		//	System.out.println(lvx + "," + lvy + "   " + pvx + "," + pvy);

			double diffCos = Math.cos(diffAng);
			double diffSin = Math.sin(diffAng);
			double newx = last[0], newy = last[1];
			for (int i = 0; i < cnt; ++i) {
				double[] next = history.get(minPosition + learn + i + 1);
				double dx = next[0] - prev[0];
				double dy = next[1] - prev[1];
		//		System.out.println(dx + ":" + dy + "->" + (diffCos * dx - diffSin * dy) + ":" + (diffSin * dx + diffCos * dy));
				newx = newx + (diffCos * dx - diffSin * dy);
				newy = newy + (diffSin * dx + diffCos * dy);
				result[i][0] = newx; 
				result[i][1] = newy;
				prev = next;
			}

			if (g != null) {

			System.out.print("d2 = [ ");
			for (int i = 0; i < learn; ++i) {
				System.out.print("[" + df.format(history.get(history.size() - learn + i)[0]) + "," + df.format(history.get(history.size() - learn + i)[1]) + "],");
				g.setColor(new Color(0, 50 + i * 5, 0));
				g.drawOval((int)history.get(history.size() - learn + i)[0] - 3, (int)history.get(history.size() - learn + i)[1] - 3, 6, 6);				
			}
			System.out.println("[0, 0]]");

			g.setColor(Color.red);
			System.out.print("d1 = [ ");
			for (int i = 0; i < learn + cnt; ++i) {
				System.out.print("[" + df.format(history.get(minPosition + i + 1)[0]) + "," + df.format(history.get(minPosition + i + 1)[1]) + "],");
				g.setColor(new Color(50 + (int)(i * 2.5), 0, 0, 128));
				g.drawOval((int)history.get(minPosition + i + 1)[0] - 3, (int)history.get(minPosition + i + 1)[1] - 3, 6, 6);				
			}
			System.out.println("[0, 0]]");

			System.out.print("d3 = [ ");
			for (int i = 0; i < cnt; ++i) {
				System.out.print("[" + df.format(result[i][0]) + "," + df.format(result[i][1]) + "],");
				g.setColor(new Color(0, 0, 50 + i * 5));
				g.drawOval((int)result[i][0] - 3, (int)result[i][1] - 3, 6, 6);				
			}
			System.out.println("[0, 0]]");
			}
		}


		return result;
	}
*/
	public void Init() {
		ActualHistorySize = 0;
		history.add(new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE});
//		factorHistory.add(new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE});

//		while (history.size() > 2000) {
//			history.remove(0);
//			factorHistory.remove(0);
//		}

//		System.out.println(history.size());
	}

	public void EndTurn() {		
		lastenergy = energy;
		seeNow = false;
	}

	public boolean IsEnergyDrop() {
		double energyDiff = lastenergy - energy;
		return (energyDiff > 0.1 - 1e-3 && energyDiff < 3 + 1e-3);
	}

	public void Log(ScannedRobotEvent ev) {
		seeNow = true;
		energy = ev.getEnergy();
		distance = ev.getDistance();
		heading = ev.getHeadingRadians();
		bearing = ev.getBearingRadians();
		velocity = ev.getVelocity();
		if (velocity < 0) {
			velocity = -velocity;
			heading = Utils.normalAbsoluteAngle(Math.PI + heading);
		}
		x = Math.sin(me.getHeadingRadians() + bearing) * distance + me.getX();
		y = Math.cos(me.getHeadingRadians() + bearing) * distance + me.getY();
	//	CalcFactors();
	//	history.add(new double[]{x, y, velocity * Math.sin(heading), velocity * Math.cos(heading), velocity});
	//	++ActualHistorySize;
	}
}																																																									