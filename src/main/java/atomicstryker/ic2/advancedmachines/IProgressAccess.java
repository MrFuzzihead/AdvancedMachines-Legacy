package atomicstryker.ic2.advancedmachines;

/**
 * Exposes the raw progress counter of an IC2 {@code TileEntityStandardMachine} to the shared
 * {@link CommonLogicAdvancedMachines} helper.
 * <p>
 * IC2 declares {@code progress} as {@code protected short}, so every advanced machine subclass
 * already has direct access to it; this interface only forwards that access to the helper and
 * replaces the reflective field lookup the mod used to perform.
 */
public interface IProgressAccess {

    short getRawProgress();

    void setRawProgress(short value);
}
