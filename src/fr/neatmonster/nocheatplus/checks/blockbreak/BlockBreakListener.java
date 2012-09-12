package fr.neatmonster.nocheatplus.checks.blockbreak;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/*
 * M#"""""""'M  dP                   dP       M#"""""""'M                             dP       
 * ##  mmmm. `M 88                   88       ##  mmmm. `M                            88       
 * #'        .M 88 .d8888b. .d8888b. 88  .dP  #'        .M 88d888b. .d8888b. .d8888b. 88  .dP  
 * M#  MMMb.'YM 88 88'  `88 88'  `"" 88888"   M#  MMMb.'YM 88'  `88 88ooood8 88'  `88 88888"   
 * M#  MMMM'  M 88 88.  .88 88.  ... 88  `8b. M#  MMMM'  M 88       88.  ... 88.  .88 88  `8b. 
 * M#       .;M dP `88888P' `88888P' dP   `YP M#       .;M dP       `88888P' `88888P8 dP   `YP 
 * M#########M                                M#########M                                      
 * 
 * M""MMMMMMMM oo            dP                                       
 * M  MMMMMMMM               88                                       
 * M  MMMMMMMM dP .d8888b. d8888P .d8888b. 88d888b. .d8888b. 88d888b. 
 * M  MMMMMMMM 88 Y8ooooo.   88   88ooood8 88'  `88 88ooood8 88'  `88 
 * M  MMMMMMMM 88       88   88   88.  ... 88    88 88.  ... 88       
 * M         M dP `88888P'   dP   `88888P' dP    dP `88888P' dP       
 * MMMMMMMMMMM                                                        
 */
/**
 * Central location to listen to events that are relevant for the block break checks.
 * 
 * @see BlockBreakEvent
 */
public class BlockBreakListener implements Listener {

    /** The direction check. */
    private final Direction direction = new Direction();

    /** The fast break check (per block breaking speed). */
    private final FastBreak fastBreak = new FastBreak();
    
    /** The frequency check (number of blocks broken) */
    private final Frequency frequency = new Frequency();

    /** The no swing check. */
    private final NoSwing   noSwing   = new NoSwing();

    /** The reach check. */
    private final Reach     reach     = new Reach();
    
    /** The wrong block check. */
    private final WrongBlock wrongBlock = new WrongBlock();

    /**
     * We listen to BlockBreak events for obvious reasons.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        /*
         *  ____  _            _      ____                 _    
         * | __ )| | ___   ___| | __ | __ ) _ __ ___  __ _| | __
         * |  _ \| |/ _ \ / __| |/ / |  _ \| '__/ _ \/ _` | |/ /
         * | |_) | | (_) | (__|   <  | |_) | | |  __/ (_| |   < 
         * |____/|_|\___/ \___|_|\_\ |____/|_|  \___|\__,_|_|\_\
         */
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        boolean cancelled = false;

        // Do the actual checks, if still needed. It's a good idea to make computationally cheap checks first, because
        // it may save us from doing the computationally expensive checks.
        
        final BlockBreakConfig cc = BlockBreakConfig.getConfig(player);
        final BlockBreakData data = BlockBreakData.getData(player);
        final long now = System.currentTimeMillis();
        
        // Has the player broken a block that was not damaged before?
        if (wrongBlock.isEnabled(player) && wrongBlock.check(player, block, cc, data))
        	cancelled = true;

        // Has the player broken more blocks per second than allowed?
        if (!cancelled && frequency.isEnabled(player) && frequency.check(player, cc, data))
        	cancelled = true;
        
        // Has the player broken blocks faster than possible?
        if (!cancelled && fastBreak.isEnabled(player) && fastBreak.check(player, block, cc, data))
            cancelled = true;

        // Did the arm of the player move before breaking this block?
        if (!cancelled && noSwing.isEnabled(player) && noSwing.check(player, data))
            cancelled = true;

        // Is the block really in reach distance?
        if (!cancelled && reach.isEnabled(player) && reach.check(player, block.getLocation(), data))
            cancelled = true;

        // Did the player look at the block at all?
        if (!cancelled && direction.isEnabled(player) && direction.check(player, block.getLocation(), data))
            cancelled = true;

        // At least one check failed and demanded to cancel the event.
        if (cancelled){
        	event.setCancelled(cancelled);
        	// Reset damage position:
    		data.fastBreakfirstDamage = now;
    		data.clickedX = block.getX();
    		data.clickedY = block.getY();
    		data.clickedZ = block.getZ();
        }
        else{
        	// Invalidate last damage position:
        	data.clickedX = Integer.MAX_VALUE;
        }
        
    }

    /**
     * We listen to PlayerAnimation events because it is (currently) equivalent to "player swings arm" and we want to
     * check if he did that between block breaks.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            priority = EventPriority.MONITOR)
    public void onPlayerAnimation(final PlayerAnimationEvent event) {
        /*
         *  ____  _                            _          _                 _   _             
         * |  _ \| | __ _ _   _  ___ _ __     / \   _ __ (_)_ __ ___   __ _| |_(_) ___  _ __  
         * | |_) | |/ _` | | | |/ _ \ '__|   / _ \ | '_ \| | '_ ` _ \ / _` | __| |/ _ \| '_ \ 
         * |  __/| | (_| | |_| |  __/ |     / ___ \| | | | | | | | | | (_| | |_| | (_) | | | |
         * |_|   |_|\__,_|\__, |\___|_|    /_/   \_\_| |_|_|_| |_| |_|\__,_|\__|_|\___/|_| |_|
         *                |___/                                                               
         */
        // Just set a flag to true when the arm was swung.
        BlockBreakData.getData(event.getPlayer()).noSwingArmSwung = true;
    }

    /**
     * We listen to BlockInteract events to be (at least in many cases) able to distinguish between block break events
     * that were triggered by players actually digging and events that were artificially created by plugins.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        /*
         *  ____  _                         ___       _                      _   
         * |  _ \| | __ _ _   _  ___ _ __  |_ _|_ __ | |_ ___ _ __ __ _  ___| |_ 
         * | |_) | |/ _` | | | |/ _ \ '__|  | || '_ \| __/ _ \ '__/ _` |/ __| __|
         * |  __/| | (_| | |_| |  __/ |     | || | | | ||  __/ | | (_| | (__| |_ 
         * |_|   |_|\__,_|\__, |\___|_|    |___|_| |_|\__\___|_|  \__,_|\___|\__|
         *                |___/                                                  
         */
    	final Player player = event.getPlayer();
    	
    	// The following is to set the "first damage time" for a block.
    	
    	// Return if it is not left clicking a block. 
    	// (Allows right click to be ignored.) 
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        final long now = System.currentTimeMillis();
        final BlockBreakData data = BlockBreakData.getData(player); 
        
        if (event.isCancelled()){
        	// Reset the time, to avoid certain kinds of cheating.
        	data.fastBreakfirstDamage = now;
        	data.clickedX = Integer.MAX_VALUE; // Should be enough to reset that one.
        	return;
        }
    	
        // Do not care about null blocks.
    	final Block block = event.getClickedBlock();
        if (block == null)
            return;
        
//        if (data.clickedX == block.getX() && data.clickedZ == block.getZ() && data.clickedY == block.getY()) return;
        // Only record first damage:
        data.fastBreakfirstDamage = now;
        // Also set last clicked blocks position.
        data.clickedX = block.getX();
        data.clickedY = block.getY();
        data.clickedZ = block.getZ();
    }
    
}
