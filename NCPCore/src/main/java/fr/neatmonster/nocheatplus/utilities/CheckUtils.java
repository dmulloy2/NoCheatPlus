package fr.neatmonster.nocheatplus.utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ICheckConfig;
import fr.neatmonster.nocheatplus.checks.access.ICheckData;
import fr.neatmonster.nocheatplus.checks.blockbreak.BlockBreakData;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.fight.FightData;
import fr.neatmonster.nocheatplus.checks.inventory.InventoryData;
import fr.neatmonster.nocheatplus.hooks.APIUtils;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.logging.Streams;

/**
 * Random auxiliary gear, some might have general quality. Contents are likely to get moved to other classes.
 */
public class CheckUtils {

    /**
     * Kick and log.
     * @param player
     */
    public static void kickIllegalMove(final Player player){
        player.kickPlayer("Illegal move.");
        StaticLog.logWarning("[NCP] Disconnect " + player.getName() + " due to illegal move!");
    }

    /**
     * Guess some last-action time, likely to be replaced with centralized PlayerData use.
     * @param player
     * @param Timestamp of the moment of calling this.
     * @param maxAge Maximum age in milliseconds.
     * @return Return timestamp or Long.MIN_VALUE if not possible or beyond maxAge.
     */
    public static final long guessKeepAliveTime(final Player player, final long now, final long maxAge){
        final int tick = TickTask.getTick();
        long ref = Long.MIN_VALUE;
        // Estimate last fight action time (important for gode modes).
        final FightData fData = FightData.getData(player); 
        ref = Math.max(ref, fData.speedBuckets.lastUpdate());
        ref = Math.max(ref, now - 50L * (tick - fData.lastAttackTick)); // Ignore lag.
        // Health regain (not unimportant).
        ref = Math.max(ref, fData.regainHealthTime);
        // Move time.
        ref = Math.max(ref, CombinedData.getData(player).lastMoveTime);
        // Inventory.
        final InventoryData iData = InventoryData.getData(player);
        ref = Math.max(ref, iData.lastClickTime);
        ref = Math.max(ref, iData.instantEatInteract);
        // BlcokBreak/interact.
        final BlockBreakData bbData = BlockBreakData.getData(player);
        ref = Math.max(ref, bbData.frequencyBuckets.lastUpdate());
        ref = Math.max(ref, bbData.fastBreakfirstDamage);
        // TODO: More, less ...
        if (ref > now || ref < now - maxAge){
            return Long.MIN_VALUE;
        }
        return ref;
    }

    /**
     * Check getPassenger recursively until a player is found, return that one or null.
     * @param entity
     * @return
     */
    public static Player getFirstPlayerPassenger(final Entity entity) {
        Entity passenger = entity.getPassenger();
        while (passenger != null){
            if (passenger instanceof Player){
                return (Player) passenger;
            }
            passenger = passenger.getPassenger();
        }
        return null;
    }

    /**
     * Check recursively for vehicles, returns null if players are vehicles, otherwise the lowest vehicle (that has no vehicle).
     * @param entity
     * @return
     */
    public static Entity getLastNonPlayerVehicle(final Entity entity) {
        Entity vehicle = entity.getVehicle();
        while (vehicle != null){
            if (vehicle instanceof Player){
                return null;
            }
            vehicle = vehicle.getVehicle();
        }
        return vehicle;
    }

    /**
     * Check for NaN, infinity.
     * @param floats
     * @return
     */
    public static boolean isBadCoordinate(float ... floats) {
        for (int i = 0; i < floats.length; i++) {
            if (Float.isNaN(floats[i]) || Float.isInfinite(floats[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for NaN, infinity.
     * @param doubles
     * @return
     */
    public static boolean isBadCoordinate(double ... doubles) {
        for (int i = 0; i < doubles.length; i++) {
            final double x = doubles[i];
            if (Double.isNaN(x) || Double.isInfinite(x) || Math.abs(x) > 3.2E7D) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for config flag and exemption (hasBypass). Meant thread-safe.
     * 
     * @param checkType
     * @param player
     * @param data
     *            If data is null, the data factory will be used for the given
     *            check type.
     * @param cc
     *            If config is null, the config factory will be used for the
     *            given check type.
     * @return
     */
    public static boolean isEnabled(final CheckType checkType, final Player player, final ICheckData data, final ICheckConfig cc) {
        if (cc == null) {
            if (!checkType.isEnabled(player)) {
                return false;
            }
        }
        else if (!cc.isEnabled(checkType)) {
            return false;
        }
        return !hasBypass(checkType, player, data);
    }

    /**
     * Check for exemption by permissions, API access, possibly other. Meant
     * thread-safe.
     * 
     * @param player
     * @param checkType
     * @param data
     *            If data is null, the data factory will be used for the given
     *            check type.
     * @return
     */
    public static boolean hasBypass(final CheckType checkType, final Player player, final ICheckData data) {
        // TODO: Checking for the thread might be a temporary measure.
        final String permission =  checkType.getPermission();
        if (Bukkit.isPrimaryThread()) {
            if (permission != null && player.hasPermission(permission)) {
                return true;
            }
        } else if (permission != null) {
            if (data == null) {
                if (checkType.hasCachedPermission(player, permission)) {
                    return true;
                }
            }
            else if (data.hasCachedPermission(permission)) {
                return true;
            }
            if (!APIUtils.needsSynchronization(checkType)) {
                // Checking for exemption can cause harm now.
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().severe(Streams.STATUS, "Off primary thread call to hasByPass for " + checkType + ".");
            }
        }
        // TODO: ExemptionManager relies on the initial definition for which type can be checked off main thread.
        return NCPExemptionManager.isExempted(player, checkType);
    }

}
