package dev.truewinter.minebroadcast.handlers;

import dev.truewinter.minebroadcast.MineBroadcast;
import dev.truewinter.minebroadcast.SafeBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is necessary because people are dumb enough to use pistons to
 * change block position and retrigger a broadcast
 *
 * @author bendem
 * @author TrueWinter
 */
// TODO: Look into this further and determine whether it's actually needed
public class PistonListener implements Listener {

    private final MineBroadcast plugin;
    private final PistonUtil pistonUtil;

    public PistonListener(MineBroadcast plugin) {
        this.plugin = plugin;
        pistonUtil = new PistonUtil();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        // Workaround start - BlockPistonEvent's are sometime called multiple times
        if(!pistonUtil.canRetract(e.getBlock())) {
            return;
        }
        pistonUtil.retract(e.getBlock());
        // Workaround end

        List<Block> blocksMoving = e.getBlocks();
        if (blocksMoving.size() == 0) return;

        Block blockMoving = blocksMoving.get(0);
        if(!e.isSticky() || !plugin.isWhitelisted(blockMoving.getType()) || !plugin.isWorldWhitelisted(e.getBlock().getWorld())) {
            return;
        }

        plugin.unBlackList(blockMoving);
        plugin.blackList(blockMoving.getRelative(e.getDirection().getOppositeFace()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        // Workaround start - BlockPistonEvent's are sometime called multiple times
        if(!pistonUtil.canExtend(e.getBlock())) {
            return;
        }
        pistonUtil.extend(e.getBlock());
        // Workaround end

        if(!plugin.isWorldWhitelisted(e.getBlock().getWorld())
                || e.getBlock().getRelative(e.getDirection()).getType() == Material.AIR) {
            return;
        }

        java.util.List<Block> blocks = e.getBlocks();
        for(int i = blocks.size() - 1; i >= 0; i--) {
            Block block = blocks.get(i);
            if(plugin.isWhitelisted(block.getType()) && plugin.isBlackListed(block)) {
                plugin.unBlackList(block);
                plugin.blackList(block.getRelative(e.getDirection()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonBreak(BlockBreakEvent e) {
        if(e.getBlock().getType() == Material.PISTON) {
            pistonUtil.remove(e.getBlock());
        }
    }

    private class PistonUtil {

        // Contains Map<piston, is extended>
        private final Map<SafeBlock, Boolean> pistons;

        private PistonUtil() {
            pistons = new HashMap<>();
        }

        public void retract(Block block) {
            pistons.put(new SafeBlock(block), false);
        }

        public void extend(Block block) {
            pistons.put(new SafeBlock(block), true);
        }

        public void remove(Block block) {
            pistons.remove(new SafeBlock(block));
        }

        public boolean canRetract(Block block) {
            SafeBlock safeBlock = new SafeBlock(block);
            return pistons.get(safeBlock) == null || pistons.get(safeBlock);
        }

        public boolean canExtend(Block block) {
            SafeBlock safeBlock = new SafeBlock(block);
            return pistons.get(safeBlock) == null || !pistons.get(safeBlock);
        }
    }

}
