package qwaker00;

import robocode.AdvancedRobot;
import robocode.RoundEndedEvent;
import robocode.util.Utils;

class Radar {
    private AdvancedRobot robot;
    private EnemyState enemy;
    private int scanDirection = 1;

    public Radar(AdvancedRobot parent) {
        robot = parent;
    }

    public void run() {
        if (enemy == null) {
            robot.setTurnRadarRight(45 * scanDirection);
        } else {
            double diffAngle = Utils.normalRelativeAngle(
                  + robot.getHeadingRadians()
                  - robot.getRadarHeadingRadians()
                  + enemy.bearing
            );
            if (diffAngle < 0)
                scanDirection = -1;
            else
                scanDirection = 1;
            robot.setTurnRadarRightRadians(2 * diffAngle);
            enemy = null;
        }
    }

    public void onScannedRobot(EnemyState e) {
        enemy = e;
    }

    public void onRoundEnded(RoundEndedEvent event) {
        scanDirection = 1;
    }
}
