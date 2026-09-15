package api.ntm1of90.compat.fluid.registry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import api.ntm1of90.compat.fluid.render.ColoredForgeFluid;
import api.ntm1of90.compat.fluid.render.FluidAtlasSprite;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fluids.Fluid;

/*
 * Stitches every NTM fluid into the block atlas via a custom loader sprite, sourced from the
 * fluid's GUI texture. Icons are only applied to NTM-owned fluids (ColoredForgeFluid) so
 * fluids provided by other mods keep their own textures.
 */
public class FluidRegistry {

    private static final Map<String, IIcon> stillIcons = new HashMap<>();
    private static final Map<String, IIcon> flowingIcons = new HashMap<>();
    private static final Map<String, IIcon> inventoryIcons = new HashMap<>();

    // fluids that got a sprite registered in the current stitch run
    private static final Set<FluidType> preparedFluids = new HashSet<>();

    private static boolean registered = false;

    public static void initialize() {
        if (registered) return;
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide() != Side.CLIENT) return;
        registered = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new FluidRegistry());
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) return; // fluid icons live in the block atlas

        preparedFluids.clear();

        for (FluidType type : Fluids.getAll()) {
            if (!isValid(type)) continue;
            if (!(FluidMappingRegistry.getForgeFluid(type) instanceof ColoredForgeFluid)) continue;

            String spriteName = FluidAtlasSprite.getSpriteName(type);
            event.map.setTextureEntry(spriteName, new FluidAtlasSprite(spriteName, type));
            preparedFluids.add(type);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0) return;

        for (FluidType type : preparedFluids) {
            Fluid fluid = FluidMappingRegistry.getForgeFluid(type);
            if (!(fluid instanceof ColoredForgeFluid)) continue;

            IIcon icon = event.map.getAtlasSprite(FluidAtlasSprite.getSpriteName(type));

            // static texture, so still, flowing and inventory icons all use the same sprite
            fluid.setIcons(icon, icon);

            String key = type.getName().toLowerCase(Locale.US);
            stillIcons.put(key, icon);
            flowingIcons.put(key, icon);
            inventoryIcons.put(key, icon);
        }
    }

    // special fluids and NONE are not exposed to the Forge fluid system
    private static boolean isValid(FluidType type) {
        return type != Fluids.NONE && !type.hasNoContainer() && !type.hasNoID();
    }

    @SideOnly(Side.CLIENT)
    public static IIcon getStillIcon(String fluidName) {
        return stillIcons.get(fluidName.toLowerCase(Locale.US));
    }

    @SideOnly(Side.CLIENT)
    public static IIcon getFlowingIcon(String fluidName) {
        return flowingIcons.get(fluidName.toLowerCase(Locale.US));
    }

    @SideOnly(Side.CLIENT)
    public static IIcon getInventoryIcon(String fluidName) {
        return inventoryIcons.get(fluidName.toLowerCase(Locale.US));
    }
}
