package api.ntm1of90.compat.ae2;

import cpw.mods.fml.common.Loader;

/**
 * No-op hook: rv3-beta-1045 renders via ColoredForgeFluid overrides and
 * interacts via IFluidHandler - no AE2-side registration needed.
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

        System.out.println("[NTM] AE2 compatibility initialized (passive mode)");
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
