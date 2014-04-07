package qwaker00;

import robocode.Rules;
import robocode.util.Utils;

import java.awt.*;

class Wave {
    double bulletSpeed, heading, maxAngle, startX, startY, gunHeading;
    GFHolder result;
    int direction;
    long startTime;


    public Wave(double startX, double startY, double bulletPower, long startTime) {
        this.startX = startX;
        this.startY = startY;
        this.bulletSpeed = Rules.getBulletSpeed(bulletPower);
        this.startTime = startTime;
        this.result = null;
    }

    public Wave(double startX, double startY, double bulletPower, double heading, double maxAngle, int direction, GFHolder result, long startTime, double gunHeading) {
        this.startX = startX;
        this.startY = startY;
        this.direction = direction;
        this.bulletSpeed = Rules.getBulletSpeed(bulletPower);
        this.startTime = startTime;
        this.heading = heading;
        this.maxAngle = maxAngle;
        this.result = result;
        this.gunHeading = gunHeading;
    }

    public double calcX(long time) {
        double r = bulletSpeed * (time - startTime) - 15;
        return Math.sin(gunHeading) * r + startX;
    }

    public double calcY(long time) {
        double r = bulletSpeed * (time - startTime) - 15;
        return Math.cos(gunHeading) * r + startY;
    }

    public boolean Check(double eX, double eY, long time) {
        if ( (Math.sqrt((eX - startX)*(eX - startX) + (eY - startY)*(eY - startY)) - 15) / bulletSpeed <= time - startTime) {
            if (result != null) {
                double diffAngle = direction * Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(eY - startY, eX - startX) - heading);
                int gfIndex = (int)((result.length() - 1) / 2 + Math.round((result.length() - 1) / 2 * Math.min(1., Math.max(-1., (diffAngle / maxAngle)))));
                result.Hit(gfIndex);
            }
            return true;
        }
        return false;
    }

    void Paint(Graphics g, long time) {
        if (result == null) {
            g.setColor(Color.white);
            int r = (int)Math.round(bulletSpeed * (time - startTime)) - 15;
            g.drawOval((int)startX - r, (int)startY - r, r + r, r + r);
        } else {
            g.setColor(Color.white);
            int r = (int)Math.round(bulletSpeed * (time - startTime)) - 15;
            g.drawOval((int)startX - r, (int)startY - r, r + r, r + r);
            g.drawLine((int)startX, (int)startY, (int)(Math.sin(heading) * r + startX), (int)(Math.cos(heading) * r + startY));
            g.setColor(Color.green);
            g.drawLine((int)startX, (int)startY, (int)(Math.sin(heading + maxAngle * direction) * r + startX), (int)(Math.cos(heading + maxAngle * direction) * r + startY));
            g.setColor(Color.red);
            g.drawLine((int)startX, (int)startY, (int)(Math.sin(heading - maxAngle * direction) * r + startX), (int)(Math.cos(heading - maxAngle * direction) * r + startY));
        }
    }
}
