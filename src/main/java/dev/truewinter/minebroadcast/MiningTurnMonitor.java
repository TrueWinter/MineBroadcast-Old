package dev.truewinter.minebroadcast;

import dev.truewinter.minebroadcast.events.MiningTurnMonitorEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author TrueWinter
 */
public class MiningTurnMonitor {
    // If player mines at least MIN_DIRECTION_MINED_BLOCKS blocks in one direction,
    // then suddenly mines in a different direction and finds
    // a monitored block, make note of this. If the player
    // does this 3 times, alert mods.

    private Player player;
    private int lastBlockX;
    private int lastBlockY;
    private int lastBlockZ;
    private long lastBlockTime;
    private MiningDirection lastMiningDirection;
    private int blocksMinedInDirection = 1;
    private Location playerLastLocation;
    private MiningTurnMonitorEvent miningTurnMonitorEvent;
    private boolean isEnabled = false;

    private final int PLAYER_MOVE_LIMIT = 100;
    private final int MIN_DIRECTION_MINED_BLOCKS = 5;
    private final int MAX_DIRECTION_MINED_BLOCKS = 50;

    public MiningTurnMonitor(MineBroadcast plugin, Player player) {
        this.player = player;
        isEnabled = plugin.getConfig().getBoolean("suspiciousMiningMonitorEnabled");
    }

    public void setMiningTurnMonitorEvent(MiningTurnMonitorEvent miningTurnMonitorEvent) {
        if (!isEnabled) return;
        this.miningTurnMonitorEvent = miningTurnMonitorEvent;
    }

    // any block
    public void setLastBlockPos(int x, int y, int z) {
        if (!isEnabled) return;

        // Ignore same y level
        if (this.lastBlockX == x && this.lastBlockZ == z) return;

        this.lastBlockX = x;
        this.lastBlockY = y;
        this.lastBlockZ = z;
        this.lastBlockTime = System.currentTimeMillis() / 1000L;

        Location playerLocation = player.getLocation();
        if (playerLastLocation == null) {
            playerLastLocation = player.getLocation();
        }

        MiningDirection miningDirection = calculateDirection(playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ());

        // These checks run before blocksMinedInDirection is modified. This is important,
        // as any monitored blocks within MIN_DIRECTION_MINED_BLOCKS blocks would otherwise be missed.
        if (miningTurnMonitorEvent != null) {
            // important: only fire this event if the player has mined more than MIN_DIRECTION_MINED_BLOCKS.
            // this helps prevent false positives, especially when caving.
            if (blocksMinedInDirection >= MIN_DIRECTION_MINED_BLOCKS && !miningDirection.equals(lastMiningDirection)) {
                // blocksMinedInDirection is updated later
                miningTurnMonitorEvent.didTurnEvent();
            }

            if (blocksMinedInDirection >= MAX_DIRECTION_MINED_BLOCKS) {
                miningTurnMonitorEvent.didMineForLongAfterTurn();
                blocksMinedInDirection = MIN_DIRECTION_MINED_BLOCKS;
            }
        }

        // If the player moved more than PLAYER_MOVE_LIMIT blocks away,
        // they're probably breaking blocks elsewhere (like at their base).
        if (hasPlayerMovedTooFarAway(playerLocation)) {
            if (lastMiningDirection == miningDirection) {
                // this is incremented later, so set it to 0 here
                blocksMinedInDirection = 0;
            } else {
                blocksMinedInDirection = 1;
            }

            miningTurnMonitorEvent.resetTurnMonitor();
        }

        playerLastLocation = player.getLocation();

        if (lastMiningDirection == miningDirection) {
            blocksMinedInDirection++;
        } else {
            blocksMinedInDirection = 1;
        }

        setLastMiningDirection(miningDirection);

        //System.out.println(miningDirection);
        //System.out.println(blocksMinedInDirection);
    }

    private MiningDirection calculateDirection(int x, int y, int z) {
        //System.out.println("X: " + lastBlockX + " Z: " + lastBlockZ);
        //System.out.println("P: " + x + " " + z);

        if (z > lastBlockZ) {
            return MiningDirection.NORTH;
        } else if (x < lastBlockX) {
            return MiningDirection.EAST;
        } else if (z < lastBlockZ) {
            return MiningDirection.SOUTH;
        } else if (x > lastBlockX) {
            return MiningDirection.WEST;
        } else if (y > lastBlockY) {
            return MiningDirection.DOWN;
        } else if (y < lastBlockY) {
            return MiningDirection.UP;
        }

        return lastMiningDirection;
    }

    private void setLastMiningDirection(MiningDirection miningDirection) {
        this.lastMiningDirection = miningDirection;
    }

    private boolean hasPlayerMovedTooFarAway(Location location) {
        //System.out.println(playerLastLocation);
        //System.out.println(location);
        if (Math.abs(playerLastLocation.getBlockX() - location.getBlockX()) > PLAYER_MOVE_LIMIT ||
                Math.abs(playerLastLocation.getBlockY() - location.getBlockY()) > PLAYER_MOVE_LIMIT ||
                Math.abs(playerLastLocation.getBlockZ() - location.getBlockZ()) > PLAYER_MOVE_LIMIT) {
            return true;
        }

        return false;
    }

    public void resetTurnMonitor() {
        if (!isEnabled) return;
        resetTurnMonitor(false);
    }

    public void resetTurnMonitor(boolean noActivity) {
        if (!isEnabled) return;
        this.blocksMinedInDirection = 0;
        this.lastBlockTime = System.currentTimeMillis() / 1000L;
        emitResetTurnMonitor(noActivity);
    }

    private void emitResetTurnMonitor(boolean noActivity) {
        if (!noActivity) {
            miningTurnMonitorEvent.resetTurnMonitor();
            //System.out.println("full reset");
        } else {
            miningTurnMonitorEvent.resetTurnMonitorAfterNoActivity();
            //System.out.println("partial reset");
        }
    }

    public void resetTurnMonitorIfNoActivity() {
        if (!isEnabled) return;
        long diff = (System.currentTimeMillis() / 1000L) - (this.lastBlockTime);
        //System.out.println(diff);

        // 5 minutes
        if (diff > (5 * 60L)) {
            resetTurnMonitor(true);
        }
    }
}
