package qwaker00;

import robocode.AdvancedRobot;
import robocode.RoundEndedEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Rectangle2D;

class Move {
    private AdvancedRobot robot;
    private EnemyState enemy;
    private int moveDirection;
    private double moveDx, moveDy;
    private double firstMoveDx, firstMoveDy;

    private static final int FAR_DIST = 350;
    private static final int CLOSE_DIST = 300;

    public Move(AdvancedRobot parent) {
        robot = parent;
        moveDirection = 1;
    }

    public void run() {
        if (enemy == null) return;

        double absEnemyAngle = robot.getHeadingRadians() + enemy.bearing;
        double resultAngle = absEnemyAngle;

        double realHeading = robot.getHeadingRadians() + ((moveDirection < 0) ? Math.PI : 0);
        double relativeAng = Utils.normalRelativeAngle(realHeading - absEnemyAngle);

        boolean cw = relativeAng < 0;
        resultAngle += Math.PI / 2 * (cw ? -1 : 1);

        if (enemy.distance > FAR_DIST) {
            resultAngle += Math.PI / 6 * (cw ? 1 : -1);
        }
        if (enemy.distance < CLOSE_DIST) {
            resultAngle += Math.PI / 6 * (cw ? -1 : 1);
        }

        firstMoveDx = Math.sin(resultAngle) * 150;
        firstMoveDy = Math.cos(resultAngle) * 150;

        Rectangle2D fieldRect = new Rectangle2D.Double(36, 36, robot.getBattleFieldWidth()-72, robot.getBattleFieldHeight()-72);
        while (!fieldRect.contains(robot.getX()+Math.sin(resultAngle)*150, robot.getY()+Math.cos(resultAngle)*150))
        {
            resultAngle += moveDirection * .02;
        }

        moveDx = Math.sin(resultAngle) * 150;
        moveDy = Math.cos(resultAngle) * 150;

        double diffAngle = Utils.normalRelativeAngle(Math.PI / 2 - Math.atan2(moveDy, moveDx) + ((moveDirection < 0) ? Math.PI : 0)  - robot.getHeadingRadians());
        robot.setTurnRightRadians(diffAngle);
        robot.setAhead(100 * moveDirection);
    }

    public void onPaint(Graphics g) {
        g.setColor(Color.red);
        g.drawLine( (int)(robot.getX() + moveDx),
                (int)(robot.getY() + moveDy),
                (int)robot.getX(),
                (int)robot.getY()
        );
        g.setColor(Color.blue);
        g.drawLine( (int)(robot.getX() + firstMoveDx),
                (int)(robot.getY() + firstMoveDy),
                (int)robot.getX(),
                (int)robot.getY()
        );
    }

    public void onScannedRobot(EnemyState e) {
        enemy = e;
    }

    public void onRoundEnded(RoundEndedEvent event) {

    }
}
