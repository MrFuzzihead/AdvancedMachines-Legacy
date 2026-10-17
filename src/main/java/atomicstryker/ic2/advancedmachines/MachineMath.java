package atomicstryker.ic2.advancedmachines;

/**
 * Pure, side-effect-free calculations shared by the advanced machines.
 * <p>
 * Everything in here is deliberately free of Minecraft/IC2 types so that it can be unit tested
 * without booting a game. The in-game classes are thin wrappers around these functions.
 */
public final class MachineMath {

    private MachineMath() {}

    /**
     * Ramps the "speed" counter by one tick towards its maximum while the machine is active, or back
     * down towards zero while it is idle.
     *
     * @param speed    current speed counter
     * @param maxTicks speed value at which the machine reaches {@code maxMachineSpeedUpFactor}x speed
     * @param active   whether the base IC2 machine ran this tick
     * @return the next speed counter, always within {@code [0, max(maxTicks, 0)]}
     */
    public static int rampSpeed(int speed, int maxTicks, boolean active) {
        if (active) {
            int cap = Math.max(0, maxTicks);
            if (speed >= cap) {
                return cap;
            }
            // speed < cap <= Integer.MAX_VALUE, so speed + 1 cannot overflow
            return Math.max(0, speed + 1);
        }
        return speed <= 0 ? 0 : speed - 1;
    }

    /**
     * Number of additional progress ticks granted on top of the single tick the base IC2 machine
     * performed.
     * <p>
     * The original formula was {@code round(speed / (maxMachineSpeedUpTicks / (factor - 1)))}; because
     * every operand is an {@code int} the {@code round} was a no-op, so the integer division is kept
     * for behavioural compatibility. All degenerate configurations (non-positive factor/ticks or a
     * truncated divisor) return {@code 0} instead of throwing.
     *
     * @param speed         current speed counter
     * @param maxTicks      {@code maxMachineSpeedUpTicks} from the config
     * @param speedUpFactor {@code maxMachineSpeedUpFactor} from the config (including the base tick)
     * @return extra progress ticks in {@code [0, max(0, speedUpFactor - 1)]}
     */
    public static short extraProgressTicks(int speed, int maxTicks, int speedUpFactor) {
        int extraLevels = speedUpFactor - 1;
        if (extraLevels <= 0 || maxTicks <= 0) {
            return 0;
        }
        int ticksPerExtraProgress = maxTicks / extraLevels;
        if (ticksPerExtraProgress <= 0) {
            return 0;
        }
        int extra = Math.max(0, speed) / ticksPerExtraProgress;
        // Defensive clamp: the ramp normally caps speed at maxTicks, but setClientSpeed()/NBT can
        // feed arbitrary values.
        return (short) Math.min(extra, extraLevels);
    }

    /**
     * The speed multiplier the machine will reach at {@code maxTicks}, i.e.
     * {@code extraProgressTicks(maxTicks, ...) + 1}.
     */
    public static int effectiveSpeedUpFactor(int maxTicks, int speedUpFactor) {
        return 1 + extraProgressTicks(Math.max(0, maxTicks), maxTicks, speedUpFactor);
    }

    /** @return true when the configured speed range is sane and cannot break the ramp formula. */
    public static boolean isValidSpeedConfig(int speedUpFactor, int maxTicks) {
        return speedUpFactor >= 2 && maxTicks > 0;
    }

    /** @return true when the configured power draw multiplier is finite and positive. */
    public static boolean isValidPowerDrawFactor(double powerDrawFactor) {
        return powerDrawFactor > 0 && !Double.isNaN(powerDrawFactor) && !Double.isInfinite(powerDrawFactor);
    }

    /** Applies the configured power draw multiplier to a base EU/t value, never returning < 1. */
    public static int scaledEnergyConsume(int baseEnergyConsume, double powerDrawFactor) {
        if (baseEnergyConsume <= 0 || !isValidPowerDrawFactor(powerDrawFactor)) {
            return Math.max(1, baseEnergyConsume);
        }
        long scaled = Math.round(baseEnergyConsume * powerDrawFactor);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }
}
