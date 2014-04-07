package qwaker00;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.*;


/*
 *  All you need is love
 */

public class Gandhi extends AdvancedRobot
{
	Random R = new Random();
	ScannedRobotEvent enemy = null;
	double lastEnemyEnergy = 100, edist, sumV, cntV;

	final int MAX_SHOTS = 10;								
																		
	/*
	 * Run, Gandhi, run!
	 */	
	public void run() {
		//Init
		setColors(Color.black, Color.gray, Color.gray, Color.yellow, Color.white);
		setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
		sumV = cntV = 0;
		enemyHits = enemyShot = 0;

		while (true) {
			RadarFolower();
			BodyMover();
			GunShooter();
			
			// End turn
			if (enemy != null) lastEnemyEnergy = enemy.getEnergy();
			enemy = null;
			execute();
		}
	}



	/*
	 *  I'm watching you
	 */

	int scanDir = 1;

	void RadarFolower() {
		if(enemy == null) {
			setTurnRadarRight(45 * scanDir);
		} else {
			double diffAngle = Utils.normalRelativeAngleDegrees(getHeading()
														 		- getRadarHeading()
														 		+ enemy.getBearing()
																);
			if (diffAngle < 0) scanDir = -1; else scanDir = 1;
			setTurnRadarRight(diffAngle);
		}
	}
	

	/*
	 *  Shake your body, buddy
	 */

	int moveDir = 1, ramIndex = 0;
	double wallResistX, wallResistY, moveX, moveY;
				
	double calcResist(double dist){
		dist = Math.min(dist, 130);
		return (Math.exp( (130  - dist) / 20 ) - 1);
	}
	
	void WallResistEvent() {
		wallResistX = calcResist(getX()) - calcResist(getBattleFieldWidth() - getX());
		wallResistY = calcResist(getY()) - calcResist(getBattleFieldHeight() - getY());
	}
		
	static boolean ReverseStrategy = true;
	int enemyShot = 0, enemyHits = 0;
	
	void BodyMover() {
		WallResistEvent();
				
		if(R.nextDouble() * 100 < 2) moveDir *= -1;
							
		if (enemy != null) {
			if (enemy.getEnergy() == 0) {
				ramIndex++;
				setAhead(0);
				if (ramIndex >= 30) {
					setTurnRight(enemy.getBearing());
					if (Math.abs(enemy.getBearing()) < 1e-2) {				
						setAhead(100);
					}
				}
				return;
			} else {
				ramIndex = 0;
			}
			double energyDiff = lastEnemyEnergy - enemy.getEnergy();
			if (energyDiff > 0.1 - 1e-3 && energyDiff < 3 + 1e-3) {
				enemyShot++;
				if (enemyHits > 0.3 * enemyShot && enemyShot > MAX_SHOTS) {
					ReverseStrategy ^= true;
					enemyHits = enemyShot = 0;
				}
				if (ReverseStrategy && Math.abs(getVelocity()) > 4) {
					moveDir *= -1;
				}
			}
			
			double absEnemyAngle = getHeadingRadians() + enemy.getBearingRadians();
			double resultAngle = absEnemyAngle;
									
			double realHeading = getHeadingRadians() + ((moveDir < 0) ? Math.PI : 0);
			double relativeAng = Utils.normalRelativeAngle(realHeading - absEnemyAngle);
		
			if (relativeAng > Math.PI * 5 / 6 || relativeAng < - Math.PI * 5 / 6) {
				moveDir *= -1;
				relativeAng = Utils.normalRelativeAngle(absEnemyAngle - realHeading - Math.PI);
			}
			
			boolean cw = relativeAng < 0;
			resultAngle += Math.PI / 2 * (cw ? -1 : 1);
			
			if (edist > 200) {
				resultAngle += Math.PI / 6 * (cw ? 1 : -1);
			} 
			if (edist < 130) {
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
		
		double diffAngle = Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(moveY, moveX) + ((moveDir < 0) ? Math.PI : 0)  - getHeadingRadians());

		if (diffAngle > Math.PI * 5 / 6 || diffAngle < - Math.PI * 5 / 6) {
			moveDir *= -1;
			diffAngle = Utils.normalRelativeAngle(diffAngle + Math.PI);
		}
		setTurnRightRadians(diffAngle);		
		setAhead(100 * moveDir);
	}
	

	/*
	 *  1 shot = 44.3602943 milliliters
	 *	                           google.com	
	 */

	double predX, predY, eX, eY;
	int minStart = -1;
	double minError = 1e100;
				
	void PredictXY(double bulletPower) {
		if (enemy == null)	return;
		double DT = edist / Rules.getBulletSpeed(bulletPower), LT = 0;
		double X = eX, Y = eY, V = enemy.getVelocity() * 0.4 + 0.6 * sumV / cntV, H = enemy.getHeadingRadians();
		for (int it = 0; it < 40; ++it) {
			X += Math.sin(H) * V * DT;
			Y += Math.cos(H) * V * DT;
			LT += DT;
			DT = Math.sqrt((X - getX())*(X - getX()) + (Y - getY())*(Y - getY())) / Rules.getBulletSpeed(bulletPower) - LT;
		}
		predX = Math.max(Math.min(X, 782), 18);	
		predY = Math.max(Math.min(Y, 582), 18);	
	}
	
	void GunShooter() {
		if (enemy == null || ramIndex > 0) return;
			
		double bPower = 300 / edist;
		bPower = Math.min(getEnergy() / 3, Math.max(0.1, Math.min(3, bPower)));
		PredictXY(bPower);
		double diffAngle = Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(predY  - getY(), predX - getX()) - getGunHeadingRadians());
        setTurnGunRightRadians(diffAngle);

		if (bPower >= 0.1 && edist <= 450 && getGunHeat() == 0) {
			setFire(3);
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {	
		if(enemy == null || enemy.getDistance() > e.getDistance())
			if (e.getDistance() <= 650) {
				enemy = e;
				edist = enemy.getDistance();
				eX = getX() + edist * Math.sin(enemy.getBearingRadians() + getHeadingRadians());
				eY = getY() + edist * Math.cos(enemy.getBearingRadians() + getHeadingRadians());
				sumV += enemy.getVelocity();
				cntV ++;
			}
	}

	public void onRobotDeath(RobotDeathEvent e){
		enemy = null;
	}

	public void onDeath(DeathEvent event) {
		ReverseStrategy ^= true;
		enemyHits = enemyShot = 0;
	}
	
	public void onHitRobot(HitRobotEvent e){
		lastEnemyEnergy -= 3;
		if (getVelocity() == 0) moveDir *= -1;
	}

	public void onHitByBullet(HitByBulletEvent e){
		enemyHits++;	
		if (enemyHits > 0.3 * enemyShot && enemyShot > MAX_SHOTS) {
			ReverseStrategy ^= true;
			enemyHits = enemyShot = 0;
		}
	}
}
