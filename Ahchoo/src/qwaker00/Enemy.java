package qwaker00;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

class Enemy {
    public AdvancedRobot me;
    public double distance, heading, bearing, velocity, energy, lastenergy, x, y;
    public boolean seeNow = false;
    public String name;

    public void Hit() {
        energy -= 3;
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
        name = ev.getName();
        if (velocity < 0) {
            velocity = -velocity;
            heading = Utils.normalAbsoluteAngle(Math.PI + heading);
        }
        x = Math.sin(me.getHeadingRadians() + bearing) * distance + me.getX();
        y = Math.cos(me.getHeadingRadians() + bearing) * distance + me.getY();
    }
}
