package fr.neatmonster.nocheatplus.checks.net.protocollib;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.net.NetConfig;
import fr.neatmonster.nocheatplus.checks.net.NetData;
import fr.neatmonster.nocheatplus.utilities.TickTask;

public class AttackFrequency extends Check {

    public AttackFrequency() {
        super(CheckType.NET_ATTACKFREQUENCY);
    }

    public boolean check(final Player player, final long time, final NetData data, final NetConfig cc) {
        // Update frequency.
        data.attackFrequencySeconds.add(time, 1f);
        double maxVL = 0.0;
        float maxLimit = 0f;
        String tags = null;
        // TODO: option to normalize the vl / stats to per second? 
        // HALF
        float sum = data.attackFrequencySeconds.bucketScore(0); // HALF
        float limit = cc.attackFrequencyLimitSecondsHalf;
        if (sum - limit > maxVL) {
            maxVL = sum - limit;
            maxLimit = limit;
            tags = "sec_half";
        }
        // ONE (update sum).
        sum += data.attackFrequencySeconds.bucketScore(1);
        limit = cc.attackFrequencyLimitSecondsOne;
        if (sum - limit > maxVL) {
            maxVL = sum - limit;
            maxLimit = limit;
            tags = "sec_one";
        }
        // TWO (update sum).
        sum += data.attackFrequencySeconds.sliceScore(2, 4, 1f);
        limit = cc.attackFrequencyLimitSecondsTwo;
        if (sum - limit > maxVL) {
            maxVL = sum - limit;
            maxLimit = limit;
            tags = "sec_two";
        }
        // FOUR (update sum).
        sum += data.attackFrequencySeconds.sliceScore(4, 8, 1f);
        limit = cc.attackFrequencyLimitSecondsFour;
        if (sum - limit > maxVL) {
            maxVL = sum - limit;
            maxLimit = limit;
            tags = "sec_four";
        }
        // EIGHT (update sum).
        sum += data.attackFrequencySeconds.sliceScore(8, 16, 1f);
        limit = cc.attackFrequencyLimitSecondsEight;
        if (sum - limit > maxVL) {
            maxVL = sum - limit;
            maxLimit = limit;
            tags = "sec_eight";
        }

        //        if (data.debug) {
        //            player.sendMessage("AttackFrequency: " + data.attackFrequencySeconds.toLine());
        //        }

        boolean cancel = false;
        if (maxVL > 0.0) {
            // Trigger a violation.
            final ViolationData vd = new ViolationData(this, player, maxVL, 1.0, cc.attackFrequencyActions);
            if (data.debug  || vd.needsParameters()) {
                vd.setParameter(ParameterName.PACKETS, Integer.toString((int) sum));
                vd.setParameter(ParameterName.LIMIT, Integer.toString((int) maxLimit));
                vd.setParameter(ParameterName.TAGS, tags);
            }
            if (executeActions(vd)) {
                cancel = true;
            }
            // Feed Improbable.
            TickTask.requestImprobableUpdate(player.getUniqueId(), 2f);
        }

        return cancel;
    }

}
