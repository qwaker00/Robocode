package qwaker00.Bonus;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;

import java.text.*;


public class Bonus extends AdvancedRobot
{
	static final double WALL_MARGIN = 130;
	static final double RAND_CHANGE_DIR = 2;
	static final double RAM_INDEX_MAX = 30;
	static final double FAR_DIST = 200;
	static final double CLOSE_DIST = 130;
	static final int VERY_CLOSE = 100;
	static final int VERY_FAR = 450;
	static Random R = new Random();
	static Enemy E = new Enemy();

	LinkedList<Bullet> B = new LinkedList<Bullet>();
	
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
		setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
	}

	void EndTurn() {
		E.EndTurn();
		execute();
	}


	
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

	double dist(double x, double y, double xx, double yy) {
		return Math.sqrt((x - xx) * (x - xx) + (y - yy) * (y - yy));
	}
			
	void BodyMover() {
		WallResistEvent();

		while (B.size() > 0) {
			Bullet b = B.getFirst();
			if (b.isActive() && dist(getX(), getY(), b.getX(), b.getY()) < E.distance) break;
			B.removeFirst();
		}
				
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
	

	double predX, predY;
	int minStart = -1;
	double minError = 1e100;
	int myHits = 0, myShots = 0;
				
	void PredictXY(double bulletPower) {
		if (!E.seeNow) return;
	
		double T = E.distance / Rules.getBulletSpeed(bulletPower), LT = 0;
		double X = E.x, Y = E.y, V = E.velocity, H = E.heading;
		for (int it = 0; it < 40; ++it) {
			X += Math.sin(H) * V * (T - LT);
			Y += Math.cos(H) * V * (T - LT);
			LT = T;
			T = Math.sqrt((X - getX())*(X - getX()) + (Y - getY())*(Y - getY())) / Rules.getBulletSpeed(bulletPower);
		}
		predX = X;
		predY = Y;
		
		if (predX < 18) predX = 18;
		if (predY < 18) predY = 18;
		if (predX > getBattleFieldWidth() - 18) predX = getBattleFieldWidth() - 18;
		if (predY > getBattleFieldHeight() - 18) predY = getBattleFieldHeight() - 18;
	}
	
	void GunShooter() {
		if (!E.seeNow) return;
		
		if (ramIndex > 0) {
			// Z-z-z
			return;
		}
			
		double bPower = 300 / E.distance;
		bPower = Math.min(getEnergy() / 3, Math.max(0.1, Math.min(3, bPower)));
		PredictXY(bPower);
		double diffAngle = Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(predY  - getY(), predX - getX()) - getGunHeadingRadians());
        setTurnGunRightRadians(diffAngle);

		if (E.distance <= VERY_CLOSE && getGunHeat() == 0) {
			Bullet b = setFireBullet(3);
			if (b != null) {
				B.add(b);
				myShots++;
			}
		} else{
			if (E.distance <= VERY_FAR && getGunHeat() == 0 && Math.abs(diffAngle) < Math.atan2(7, E.distance)) {
				Bullet b = setFireBullet(bPower);
				if (b != null) {
					B.add(b);
					myShots++;
				}
			}
		}
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.white);
		for (Bullet b : B) {
			g.drawLine((int)b.getX() - 5, (int)b.getY() - 5, (int)b.getX() + 5, (int)b.getY() + 5);
			g.drawLine((int)b.getX() - 5, (int)b.getY() + 5, (int)b.getX() + 5, (int)b.getY() - 5);
		}
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
	}
	
	public void onHitRobot(HitRobotEvent e){
		E.Hit();
		if (getVelocity() == 0) moveDirection *= -1;
	}

	public void onHitByBullet(HitByBulletEvent e){
		E.IHit();
	}
}

class Enemy {
	public AdvancedRobot me;
	public double distance, heading, bearing, velocity, energy, lastenergy, x, y;
	public boolean seeNow = false;

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

	public void Init() {
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
	}
}
																						