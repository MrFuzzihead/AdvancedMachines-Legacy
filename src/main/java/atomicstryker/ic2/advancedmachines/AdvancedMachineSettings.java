package atomicstryker.ic2.advancedmachines;

/**
 * The speed/power tunables shared by every advanced machine. Extracted so the calculations can be
 * unit tested without a live {@link ModAdvancedMachines} instance.
 */
public final class AdvancedMachineSettings {

    public static final int DEFAULT_SPEED_UP_FACTOR = 10;
    public static final int DEFAULT_SPEED_UP_TICKS = 10000;
    public static final double DEFAULT_POWER_DRAW_FACTOR = 3.0d;

    public static final AdvancedMachineSettings DEFAULTS = new AdvancedMachineSettings(
        DEFAULT_SPEED_UP_FACTOR,
        DEFAULT_SPEED_UP_TICKS,
        DEFAULT_POWER_DRAW_FACTOR);

    /** {@code maxMachineSpeedUpFactor}: speed multiple reached at {@link #speedUpTicks}, base tick included. */
    public final int speedUpFactor;
    /** {@code maxMachineSpeedUpTicks}: ticks of continuous operation needed to reach the top speed. */
    public final int speedUpTicks;
    /** {@code machinePowerDrawFactor}: EU/t multiplier. */
    public final double powerDrawFactor;

    public AdvancedMachineSettings(int speedUpFactor, int speedUpTicks, double powerDrawFactor) {
        this.speedUpFactor = speedUpFactor;
        this.speedUpTicks = speedUpTicks;
        this.powerDrawFactor = powerDrawFactor;
    }
}
