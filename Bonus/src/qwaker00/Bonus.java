package qwaker00;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;

public class Bonus extends AdvancedRobot {
    private volatile boolean gameRunning;
    private Radar radar;
    private Move  move;
    private Gun gun;

    public void run() {
        setColors(Color.black, Color.gray, Color.gray, Color.yellow, Color.white);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        if (radar == null) radar = new Radar(this);
        if (move == null) move = new Move(this);
        if (gun == null) gun = new Gun(this);

        gameRunning = true;
        while (gameRunning) {
            radar.run();
            move.run();
            gun.run();
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        EnemyState e = new EnemyState(event);
        radar.onScannedRobot(e);
        move.onScannedRobot(e);
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        gameRunning = false;
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        radar.onRoundEnded(event);
        move.onRoundEnded(event);
        gun.onRoundEnded(event);
    }

    @Override
    public void onPaint(Graphics2D g) {
        move.onPaint(g);
    }

    static public void main(String[] args) {
        Bonus b = new Bonus();
        b.run();
    }
}
