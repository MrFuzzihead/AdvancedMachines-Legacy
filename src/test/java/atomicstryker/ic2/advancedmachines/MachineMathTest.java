package atomicstryker.ic2.advancedmachines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Unit tests for the pure speed/energy calculations. These mirror the behaviour of the original
 * (integer-division) speed formula and pin down the degenerate-config handling.
 */
public class MachineMathTest {

    /** The mod's default config values. */
    private static final int FACTOR = 10;
    private static final int TICKS = 10000;

    @Test
    public void rampSpeedClimbsByOneWhileActive() {
        assertEquals(1, MachineMath.rampSpeed(0, TICKS, true));
        assertEquals(500, MachineMath.rampSpeed(499, TICKS, true));
        assertEquals(TICKS, MachineMath.rampSpeed(TICKS - 1, TICKS, true));
    }

    @Test
    public void rampSpeedDecaysByOneWhileIdle() {
        assertEquals(0, MachineMath.rampSpeed(0, TICKS, false));
        assertEquals(0, MachineMath.rampSpeed(1, TICKS, false));
        assertEquals(9, MachineMath.rampSpeed(10, TICKS, false));
    }

    @Test
    public void rampSpeedClampsToValidRange() {
        assertEquals(TICKS, MachineMath.rampSpeed(TICKS, TICKS, true));
        assertEquals(TICKS, MachineMath.rampSpeed(Integer.MAX_VALUE, TICKS, true));
        assertEquals(0, MachineMath.rampSpeed(Integer.MIN_VALUE, TICKS, false));
        assertEquals(0, MachineMath.rampSpeed(-5, TICKS, true));
    }

    @Test
    public void extraProgressMatchesOriginalIntegerFormula() {
        // ticksPerExtraProgress = maxMachineSpeedUpTicks / (factor - 1) = 10000 / 9 = 1111
        assertEquals(0, MachineMath.extraProgressTicks(0, TICKS, FACTOR));
        assertEquals(0, MachineMath.extraProgressTicks(1110, TICKS, FACTOR));
        assertEquals(1, MachineMath.extraProgressTicks(1111, TICKS, FACTOR));
        assertEquals(2, MachineMath.extraProgressTicks(2222, TICKS, FACTOR));
        assertEquals(9, MachineMath.extraProgressTicks(9999, TICKS, FACTOR));
    }

    @Test
    public void extraProgressAtMaxSpeedYieldsConfiguredFactor() {
        // 1 base tick + 9 extra ticks = 10x
        assertEquals(9, MachineMath.extraProgressTicks(TICKS, TICKS, FACTOR));
        assertEquals(10, MachineMath.effectiveSpeedUpFactor(TICKS, FACTOR));
    }

    @Test
    public void extraProgressIsClampedToConfiguredLevels() {
        // setClientSpeed()/NBT can feed arbitrary values; never exceed factor-1
        assertEquals(9, MachineMath.extraProgressTicks(Integer.MAX_VALUE, TICKS, FACTOR));
        assertEquals(9, MachineMath.extraProgressTicks(100_000, TICKS, FACTOR));
    }

    @Test
    public void extraProgressHandlesDegenerateConfigs() {
        assertEquals(0, MachineMath.extraProgressTicks(10000, 10000, 1)); // factor 1 -> no extra
        assertEquals(0, MachineMath.extraProgressTicks(10000, 10000, 0));
        assertEquals(0, MachineMath.extraProgressTicks(10000, 10000, -3));
        assertEquals(0, MachineMath.extraProgressTicks(10000, 0, FACTOR)); // zero ticks
        assertEquals(0, MachineMath.extraProgressTicks(10000, -1, FACTOR));
        // maxTicks / (factor-1) truncates to zero
        assertEquals(0, MachineMath.extraProgressTicks(10000, 1, 100));
    }

    @Test
    public void extraProgressNeverNegativeForNegativeSpeed() {
        assertEquals(0, MachineMath.extraProgressTicks(-100, TICKS, FACTOR));
    }

    @Test
    public void scaledEnergyConsumeAppliesFactor() {
        assertEquals(6, MachineMath.scaledEnergyConsume(2, 3.0d));
        assertEquals(3, MachineMath.scaledEnergyConsume(1, 3.0d));
        assertEquals(2, MachineMath.scaledEnergyConsume(2, 1.0d));
        assertEquals(1, MachineMath.scaledEnergyConsume(1, 0.5d));
    }

    @Test
    public void scaledEnergyConsumeNeverGoesBelowOne() {
        assertEquals(1, MachineMath.scaledEnergyConsume(3, 0.1d)); // round(0.3) == 0 -> clamp to 1
        assertEquals(1, MachineMath.scaledEnergyConsume(0, 3.0d));
        assertEquals(1, MachineMath.scaledEnergyConsume(-4, 3.0d));
    }

    @Test
    public void scaledEnergyConsumeIgnoresInvalidFactors() {
        assertEquals(2, MachineMath.scaledEnergyConsume(2, 0.0d));
        assertEquals(2, MachineMath.scaledEnergyConsume(2, -1.0d));
        assertEquals(2, MachineMath.scaledEnergyConsume(2, Double.NaN));
        assertEquals(2, MachineMath.scaledEnergyConsume(2, Double.POSITIVE_INFINITY));
    }

    @Test
    public void scrapRollHappensOneInEight() {
        Random random = new Random(1234567L);
        int rolls = 80000;
        int hits = 0;
        for (int i = 0; i < rolls; i++) {
            if (MachineMath.rollScrap(random, 8)) {
                hits++;
            }
        }
        // p = 1/8 -> expect 10000 hits; allow ~3 standard deviations of slack
        assertTrue("hits=" + hits, hits > 9000 && hits < 11000);
    }

    @Test
    public void scrapRollAlwaysHappensForNonPositiveChance() {
        Random random = new Random(1L);
        assertTrue(MachineMath.rollScrap(random, 1));
        assertTrue(MachineMath.rollScrap(random, 0));
        assertTrue(MachineMath.rollScrap(random, -5));
    }

    @Test
    public void overclockersAboveTheCapAreExcess() {
        assertEquals(0, MachineMath.excessOverclockers(0, MachineMath.MAX_OVERCLOCKERS));
        assertEquals(0, MachineMath.excessOverclockers(6, MachineMath.MAX_OVERCLOCKERS));
        assertEquals(1, MachineMath.excessOverclockers(7, MachineMath.MAX_OVERCLOCKERS));
        assertEquals(58, MachineMath.excessOverclockers(64, MachineMath.MAX_OVERCLOCKERS));
    }

    @Test
    public void overclockersFitUntilTheCap() {
        assertTrue(MachineMath.canAddOverclockers(0, 6, MachineMath.MAX_OVERCLOCKERS));
        assertTrue(MachineMath.canAddOverclockers(5, 1, MachineMath.MAX_OVERCLOCKERS));
        assertTrue(MachineMath.canAddOverclockers(6, 0, MachineMath.MAX_OVERCLOCKERS));
        assertFalse(MachineMath.canAddOverclockers(6, 1, MachineMath.MAX_OVERCLOCKERS));
        assertFalse(MachineMath.canAddOverclockers(4, 3, MachineMath.MAX_OVERCLOCKERS));
        assertFalse(MachineMath.canAddOverclockers(0, 7, MachineMath.MAX_OVERCLOCKERS));
    }

    @Test
    public void configValidation() {
        assertTrue(MachineMath.isValidSpeedConfig(10, 10000));
        assertTrue(MachineMath.isValidSpeedConfig(2, 1));
        assertFalse(MachineMath.isValidSpeedConfig(1, 10000));
        assertFalse(MachineMath.isValidSpeedConfig(0, 10000));
        assertFalse(MachineMath.isValidSpeedConfig(10, 0));
        assertFalse(MachineMath.isValidSpeedConfig(10, -5));

        assertTrue(MachineMath.isValidPowerDrawFactor(3.0d));
        assertTrue(MachineMath.isValidPowerDrawFactor(0.5d));
        assertFalse(MachineMath.isValidPowerDrawFactor(0.0d));
        assertFalse(MachineMath.isValidPowerDrawFactor(-1.0d));
        assertFalse(MachineMath.isValidPowerDrawFactor(Double.NaN));
        assertFalse(MachineMath.isValidPowerDrawFactor(Double.POSITIVE_INFINITY));
    }
}
