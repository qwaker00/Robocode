package qwaker00;

import robocode.ScannedRobotEvent;

public class EnemyState {
    double bearing;
    double distance;

    public EnemyState(ScannedRobotEvent e) {
        bearing = e.getBearingRadians();
        distance = e.getDistance();
    }
}
