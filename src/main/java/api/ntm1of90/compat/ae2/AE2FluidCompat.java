package api.ntm1of90.compat.ae2;

import cpw.mods.fml.common.Loader;

/**
 * AE2 compatibility entry point.
 *
 * The original NTM-1-of-90 version targeted AE2 rv2/rv3-beta.56 and patched
 * rendering through reflection (appeng.client.texture.FluidRenderMap) and
 * FluidCellInventory field injection. This fork runs AE2 rv3-beta-1045-GTNH
 * where those internals no longer exist (no FluidRenderMap; CellInventory now
 * stores stacks in `cellStacks` with NBT keys "#"/"@", no `cellItems` field).
 *
 * None of that is needed anymore:
 *  - Fluid rendering: rv3-beta-1045 renders fluids via Fluid.getStillIcon() +
 *    getColor(FluidStack); ColoredForgeFluid already overrides both, so NTM
 *    fluids display correctly without any patching.
 *  - Fluid transport: import/export buses, storage buses and dual fluid
 *    interfaces interact purely through net.minecraftforge.fluids.IFluidHandler
 *    (see TileEntityDummy / AutoForgeFluidAdapter) - no AE2-side registration
 *    required.
 *
 * Kept as a no-op hook so MainRegistry wiring stays stable.
 */
public class AE2FluidCompat {

    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        if (!Loader.isModLoaded("appliedenergistics2")) {
            System.out.println("[NTM] Applied Energistics 2 not detected, skipping AE2 compatibility");
            initialized = true;
            return;
        }

        System.out.println("[NTM] AE2 compatibility initialized (passive mode: IFluidHandler bridge + ColoredForgeFluid rendering cover rv3-beta-1045)");
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
