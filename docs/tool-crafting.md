# Tool Crafting — Java Usage

`ItemToolRecipeHelper` in `com.hbm.crafting.ItemToolRecipeHelper` adds tool-based crafting recipes.

Tool-required crafting builds on the existing `com.hbm.items.tool.ItemTooling`: tools are plain `ItemTooling` instances carrying an `api.hbm.block.IToolable.ToolType` and an integer tier (e.g. `new ItemTooling(ToolType.SCREWDRIVER, 128, 1)`). Matching runs off `IToolable.ToolType.stacksForDisplay` plus a tier registry kept in `ItemTooling`, so any item can become a crafting tool simply by registering it: `ToolType.HAMMER.register(stack)`.

## Shaped

```java
ItemToolRecipeHelper.addShaped(new ItemStack(YourItem, count), new Object[] {
    "IPI",
    "I I",
    "IPI",
    'I', STEEL.plate(),
    'P', STEEL.ingot()
}, ToolType.SCREWDRIVER);
```

## Shaped with minimum tier

```java
ItemToolRecipeHelper.addShaped(new ItemStack(YourItem, count), new Object[] {
    "III",
    "IDI",
    "III",
    'I', IRON.ingot(),
    'D', "gemDiamond"
}, 2 /* minTier */, ToolType.HAMMER);
```

## Mirrored Shaped

```java
ItemToolRecipeHelper.addShapedMirrored(new ItemStack(YourItem, count), new Object[] {
    " RI",
    "IPI",
    "IB ",
    'R', Blocks.furnace,
    'I', IRON.ingot(),
    'P', Blocks.piston,
    'B', IRON.block()
}, ToolType.HAMMER, ToolType.SCREWDRIVER);
```

## Shapeless

```java
ItemToolRecipeHelper.addShapeless(new ItemStack(YourItem, count), new Object[] {
    ModItems.some_ingredient,
    "ingotIron"
}, ToolType.CUTTER);
```

## Available tool types

- `ToolType.SCREWDRIVER`
- `ToolType.HAMMER`
- `ToolType.SAW`
- `ToolType.CUTTER`
- `ToolType.WRENCH`
- `ToolType.TORCH` (welding torch / blowtorch)

## Tier system

Each tool has an integer tier. Recipes can specify a `minTier` parameter (default `0`) — only tools with tier >= `minTier` will satisfy the requirement. Known tiers:

| Tier | Example tools |
|------|--------------|
| 0    | Default (no explicit tier) |
| 1    | Iron tools |
| 2    | Steel tools |
| 9    | Desh tools (unbreakable) |

## Durability damage

Tools take durability damage each time they are used in a tool recipe. The damage is calculated as:

```
damage = max(1, uniqueIngredients + outputStackSize)
```

- **Unique ingredients**: number of distinct ingredient types in the recipe
- **Output stack size**: how many items the recipe produces

This means more complex recipes (more ingredients, larger output) consume more durability. The damage value is written to the tool's NBT under the `"craftingDurabilityDamage"` key (constant `ItemTooling.KEY_CRAFTING_DAMAGE`). Tools consume it via `ItemTooling.getPendingCraftingDamage(stack)`; ordinary crafting falls back to a single point of damage. Welding torches (blowtorch/acetylene torch) scale their fuel consumption by this same factor (`5 * damage` gas for blowtorch, `2 * damage` of each fluid for acetylene torch).
