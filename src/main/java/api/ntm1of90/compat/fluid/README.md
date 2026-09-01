# Forge Fluid Compatibility API

This API provides compatibility between HBM's Nuclear Tech Mod's custom fluid system and Forge's standard fluid system. It allows for seamless interaction between mods that use either system.

## Features

- **Automatic mapping** between HBM's FluidType and Forge's Fluid
- **Dynamic fluid discovery** - reads directly from HBM's Fluids.java using `Fluids.getAll()`
- **Adapter classes** for implementing Forge's IFluidHandler interface for HBM tile entities
- **Texture mapping** for proper rendering of HBM fluids in Forge-compatible containers
- **Automatic texture generation** based on fluid names with consistent naming patterns
- **Localization support** for fluid names
- **Color information** extracted directly from FluidType objects for accurate rendering
- **No external configuration** - eliminates dependency on JSON files

## Usage

### Basic Usage

To initialize the API, call:

```java
ForgeFluidCompatManager.initialize();
```

This should be done during mod initialization. The system will automatically:
- Discover all fluids from `Fluids.getAll()`
- Convert fluid names to lowercase
- Extract color information from FluidType objects
- Generate texture paths using the pattern `"forgefluid/" + fluidname`

### Converting between HBM and Forge fluids

To convert between HBM's FluidType and Forge's Fluid:

```java
// Convert from Forge Fluid to HBM FluidType
FluidType hbmFluid = FluidMappingRegistry.getHbmFluidType(forgeFluid);

// Convert from HBM FluidType to Forge Fluid
Fluid forgeFluid = FluidMappingRegistry.getForgeFluid(hbmFluid);
```

### Getting a Forge IFluidHandler for an HBM tile entity

To get a Forge IFluidHandler for an HBM tile entity:

```java
IFluidHandler handler = ForgeFluidAdapterRegistry.getFluidHandler(tileEntity);
```

### Registering a custom fluid mapping

To register a custom mapping between a Forge fluid name and an HBM FluidType:

```java
FluidMappingRegistry.registerFluidMapping("forge_fluid_name", hbmFluidType);
```

### Fluid Registry System

The fluid registry automatically discovers and registers all HBM fluids:

```java
// The registry automatically loads all fluids during initialization
FluidRegistry.initialize();

// Get properties for any fluid
FluidRegistry.FluidProperties properties = FluidRegistry.getFluidProperties("water");

// Access texture icons (client-side only)
IIcon stillIcon = FluidRegistry.getStillIcon("oil");
IIcon flowingIcon = FluidRegistry.getFlowingIcon("oil");
IIcon inventoryIcon = FluidRegistry.getInventoryIcon("oil");
```

**Automatic Features:**
- Discovers all fluids from `Fluids.getAll()` including custom and mod-added fluids
- Converts fluid names to lowercase for consistent texture mapping
- Extracts color information directly from FluidType objects
- Generates texture paths using the pattern: `"forgefluid/" + fluidname`
- No manual configuration required

## Package Structure

- `adapter`: Adapter classes implementing Forge's IFluidHandler for HBM tile entities
- `item`: IFluidContainerItem bridges for NTM fluid container items
- `registry`: Mapping between HBM's FluidType and Forge's Fluid, adapter registry, texture registry
- `render`: Fluid rendering (ColoredForgeFluid, color applier, texture mapper)
- `util`: Fluid conversion (1:1 mB) and localization

## Implementation Details

The API uses a non-invasive approach to provide compatibility between the two fluid systems. It does not modify HBM's core fluid system or Forge's fluid system, but instead provides a bridge between them.

The main components of the API are:

1. **FluidRegistry**: Loads fluid properties from `Fluids.getAll()` and manages texture registration
2. **FluidMappingRegistry**: Maps between HBM's FluidType and Forge's Fluid
3. **ForgeFluidAdapterRegistry**: Provides IFluidHandler for every IFluidUserMK2 tile (pressurized tanks excluded)
4. **ForgeFluidCapabilityHook**: Server-tick registration + bucket right-click handling
5. **NTMFluidContainerBridge / ItemFluidContainerEmpty / ItemBlockFluidStorage**: container bridges (see item package)
6. **ColoredForgeFluid**: Forge Fluid carrying HBM's color

## Notes

- This API is designed to work with Forge 1.7.10
- It is compatible with HBM's Nuclear Tech Mod 1.0.27 and later
- It does not replace HBM's fluid system, but provides a bridge to Forge's fluid system
