package dev.truewinter.minebroadcast.events;

/**
 * @author TrueWinter
 */
public interface MiningTurnMonitorEvent {
    void didTurnEvent();
    void didMineForLongAfterTurn();
    void resetTurnMonitor();
    void resetTurnMonitorAfterNoActivity();
}
