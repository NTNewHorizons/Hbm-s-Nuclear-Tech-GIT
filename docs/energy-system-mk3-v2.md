# NTM Energy System MK3 — Voltage, Amperage & Resistance

## Design Philosophy

The MK3 energy system introduces **Voltage (V), Amperage (I), and Resistance (R)** to Hbm's Nuclear Tech Mod. It is a **custom NTM-style** system — not a clone of GregTech, Mekanism, or Immersive Engineering.

**Key principles:**

- **Voltage = Tier** — what you can connect to what
- **Amperage = Throughput** — how much power can flow through a cable
- **Resistance = Loss** — distance penalty for transmitting power
- **Endgame power bypasses cables entirely** — Dyson swarms, DFC, and ICF use beams and bus bars, not wires
- **Selective adoption** — only new MK3 content uses the system. Legacy MK2 is untouched and interoperable via transformers
- **Full electrical physics where it matters** — Ohm's law, I²R loss, overvoltage damage, undervoltage efficiency penalties

---

## Voltage Tiers

```java
LV  (120V)    — Copper wire        — 1K HE/t      — Early game
MV  (480V)    — Gold wire          — 10K HE/t     — Mid game
HV  (1920V)   — Steel/Aluminum     — 100K HE/t    — Late game  
EV  (7680V)   — Advanced alloy     — 10M HE/t     — Advanced
SC  (30720V)  — Superconductor     — 100M HE/t    — Pre-endgame
BUS (∞)       — Bus bar blocks     — Long.MAX     — Endgame (Dyson, ICF, DFC)
```

**Rules:**
- Cables of different voltage tiers **cannot connect** to each other
- A machine only accepts cables of its rated voltage tier
- Connecting two different-tier networks requires a **Transformer**

---

## Cable Properties

| Cable Material | Tier | Voltage | Max Power (HE/t) | Loss (HE/t) | Internal Buffer | Resistance (Ω) |
|----------------|------|---------|-------------------|-------------|-----------------|----------------|
| Copper Wire | LV | 120V | 1,000 | 1/t | 100 HE | 0.01 |
| Gold Wire | MV | 480V | 10,000 | 2/t | 500 HE | 0.005 |
| Steel Wire | HV | 1920V | 100,000 | 5/t | 2,000 HE | 0.002 |
| Advanced Alloy | EV | 7680V | 10,000,000 | 10/t | 10,000 HE | 0.0005 |
| Superconductor | SC | 30720V | 100,000,000 | 0/t | 100,000 HE | 0 |
| Bus Bar | BUS | ∞ | Long.MAX_VALUE | 0/t | Long.MAX_VALUE | 0 |

### Power = Voltage × Amperage

The system calculates amperage from power:

```
Amperage (I) = Power (HE/t) / Voltage (V)
```

Example: A machine drawing 1,200 HE/t from a 480V MV network draws **2.5A**.

### Cable Loss

I²R loss applies per cable segment:

```
Loss (HE/t) = I² × R
```

Example: 10A through copper wire (R=0.01):  
Loss = 10² × 0.01 = **1 HE/t per cable block**

### Cable Overload

If the power through a cable exceeds its `MaxPower` rating:
- Cable accumulates **heat** each tick (proportional to excess)
- At threshold 1: visual feedback — cable turns orange/red, buzzing sound
- At threshold 2: cable melts, drops as item, may cause fire
- Redstone-controllable **fuse** variant available (resets on redstone pulse)

---

## Machine Voltage Behavior

Each machine stores these voltage parameters:

```java
long getVoltageNominal();   // Designed voltage (e.g. 480V for MV)
double getVoltageTolerance(); // Default: 0.2 (20% tolerance)
double getEfficiency();      // Current operating efficiency (0.0 - 1.0)
```

### Voltage Range

| Condition | Effect |
|-----------|--------|
| `V_nominal × (1 - tolerance) ≤ V_received ≤ V_nominal × (1 + tolerance)` | Normal operation, 100% efficiency |
| `V_received < V_nominal × (1 - tolerance)` | **Undervoltage**: efficiency = V_received / V_nominal, machine runs slower or stops |
| `V_received > V_nominal × (1 + tolerance)` | **Overvoltage**: `onOvervoltage()` — sparks, smoke, accumulating damage, potential explosion |

### Overvoltage Damage Formula

```
excess = V_received - V_max
damageChance = excess / V_max  // per tick
if random < damageChance:
    explosion with radius = ceil(excess / V_nominal)
```

### Undervoltage Efficiency

```
efficiency = V_received / V_nominal
actualConsumption = baseConsumption × efficiency
craftTime = baseTime / efficiency
```

---

## Network Distribution Algorithm (PowerNetMK3)

Extends `NodeNet<IEnergyReceiverMK3, IEnergyProviderMK3, PowerNodeMK3>`.

### Per-tick `update()`:

```
1. DETERMINE NETWORK TIER
   - Read voltage tier from any connected cable node
   - All cables in a valid network must share the same tier
   - If mixed tiers detected: error state, no power transfer

2. SUM PROVIDERS
   - Available power = Σ min(provider.power, provider.speed)
   - Providers can be generators OR batteries in output mode

3. SUM RECEIVERS (categorized by ConnectionPriority)
   - Demand = Σ min(receiver.maxPower - receiver.power, receiver.speed)
   - Grouped by 5 priority levels (LOWEST through HIGHEST)

4. CALCULATE NETWORK RESISTANCE
   - R_total = sum of all cable resistances in network
   - Cables in parallel contribute probabilistic reduction
     (simplified: parallel branches share load, reducing effective R)

5. DETERMINE BOTTLENECK CAPACITY
   - P_max_cable = min(MaxPower) over all cables
   - If total power > P_max_cable: cable heating triggered

6. APPLY CABLE LOSS
   - I = powerThroughNetwork / V_nominal
   - I²R_loss = I² × R_total
   - Available power after loss = powerAvailable - I²R_loss

7. DISTRIBUTE POWER (same MK2 proportional logic)
   - Iterate priority tiers HIGHEST → LOWEST
   - Within each tier: proportional split by demand weight
   - Rounding errors compensated by random provider drain

8. APPLY VOLTAGE EFFECTS TO EACH RECEIVER
   - For each receiver: voltage = V_nominal (same as network)
   - If outside tolerance: trigger undervoltage or overvoltage
```

### Key Differences from MK2

| Aspect | MK2 | MK3 |
|--------|-----|-----|
| Distribution | Unlimited, proportional | Capped by cable bottleneck |
| Loss | None | I²R loss subtracted |
| Voltage | None | Tier matching enforced |
| Overload | No effect | Cable heating → melt |
| Machine effects | Always works | Undervoltage/overvoltage |

---

## Transformer Block

The bridge between different voltage tiers (and between MK2 ↔ MK3 legacy).

**Properties:**
- **Input side**: connects to tier A network (e.g. HV)
- **Output side**: connects to tier B network (e.g. MV)
- **Directional**: one-way configurable (like the diode)
- **Configurable throughput limit**: cap on HE/t passing through
- **Configurable loss**: 0-25%, default 5%
- **Internal buffer**: stores power between ticks for smooth flow
- **Two-way transformer**: upgrade that allows bidirectional flow

### MK2/MK3 Bridge Transformer

Special variant that bridges the legacy MK2 system with MK3:
- MK2 side: implements `IEnergyReceiverMK2` / `IEnergyProviderMK2`
- MK3 side: implements `IEnergyHandlerMK3` with a configurable voltage tier
- Default: MK2 power appears as MV tier
- Allows gradual migration — old generators feed new system

---

## Bus Bar System — The Endgame Transmission Tier

For power levels beyond what cables can sanely handle (Dyson, DFC, ICF), the **Bus Bar** provides contact-only transmission.

### Design

A bus bar is a **solid block** (not a cable) that conducts power through direct contact:

```
[Dyson Converter] ⇨ [Bus Bar] ⇨ [Bus Bar] ⇨ [Bus Bar] ⇨ [BUS Transformer]
                                                             ↓
                                                       HV Cable Network
```

**Properties:**
- **UNINOS provider**: separate from regular cables (different node type)
- **Connection rule**: only connects to adjacent bus bar blocks, BUS transformers, or BUS-compatible generators/consumers
- **Max throughput**: Long.MAX_VALUE (no practical limit)
- **Resistance**: 0 (superconducting)
- **Voltage**: ∞ (no tier restriction)
- **Visual**: glowing beam connectors between adjacent blocks, animated particle stream along the bus
- **Collision**: full solid block (can walk on it)

### Construction

- Crafted from superconductor coils + dense steel plates
- Minimum length: 2 blocks (cannot stand alone)
- 90° bends supported
- T-junctions and cross-junctions supported
- 1-block air gap breaks the connection (requires line-of-sight)

### BUS Transformer

Steps bus power down to a cable tier:

```
Bus Bar ⇨ BUS Transformer(THROTTLE: 100K HE/t) ⇨ HV Cable
                                             ↓
                                      Outputs 100K HE/t
                                      to HV network
```

- Configurable output tier (LV/MV/HV/EV)
- Configurable throughput limit (1 to Long.MAX_VALUE)
- Acts as a **regulator** — only draws what the target network can handle
- Huge internal buffer absorbs burst power

---

## Long-Distance Power Transportation

The mod already has a full pylon/connector system (MK2). MK3 extends these with voltage tier ratings, wire resistance, per-wire amperage limits, and wire material selection.

### Existing Pylon Hierarchy (MK2, extended for MK3)

```
IEnergyConductorMK2/MK3
  └── TileEntityCableBaseNT/MK3
        └── TileEntityPylonBase (abstract)
              ├── TileEntityConnector      — wall mount, 10m
              ├── TileEntityConnectorSuper — wall mount, 100m
              ├── TileEntityPylon          — small pylon, 25m, SINGLE
              ├── TileEntityPylonMedium    — medium pylon, 45m, TRIPLE
              ├── TileEntityPylonLarge     — large pylon, 100m, QUAD
              └── TileEntitySubstation     — local hub, 20m, QUAD
```

### MK3 Pylon Voltage Tiers

| Pylon | Max Voltage Tier | Range | Connections | Best For |
|-------|------------------|-------|-------------|----------|
| Connector | LV (120V) | 10m | SINGLE | Building-to-building |
| Super Connector | MV (480V) | 100m | SINGLE | Long point-to-point |
| Small Pylon | MV (480V) | 25m | SINGLE | Local spur |
| Medium Pylon | HV (1920V) | 45m | TRIPLE | Inter-base backbone |
| Large Pylon | EV (7680V) | 100m | QUAD | Long-distance trunk |
| Large Pylon (SC upgrade) | SC (30720V) | 100m | QUAD | Ultra-long trunk |

### Wire Materials

Pylon wires are physical items selected during connection. The player holds a wire spool and uses `ItemWiring` to create the link.

| Wire Material | Ω/m | Max Current | Range | Crafting |
|---------------|-----|-------------|-------|----------|
| Copper | 0.01 | 10A | Short | Cheap |
| Gold | 0.005 | 25A | Medium | Moderate |
| Steel | 0.002 | 75A | Long | Moderate |
| Superconductor | 0.0001 | 10kA | Any | Expensive |
| Bus Connector | 0 | ∞ | Any | Endgame (BUS tier) |

### Transmission Loss Formula

The same I²R loss applies, scaled by distance:

```
Wire Loss (HE/t) = I² × R_wire × distance
                 = (Power / Voltage)² × (Ω_per_meter × meters)
```

**Distance penalty is explicit** — longer lines lose more power unless voltage is increased.

### Distance Optimization Table

Transmitting **50K HE/t** over **80m** through different voltage tiers:

```
LV  (120V)    I = 417A   Loss = 139,000 HE/t    ← melts instantly, 25× over copper limit
MV  (480V)    I = 104A   Loss = 8,665 HE/t      ← 4× over gold limit, wire snaps
HV  (1920V)   I = 26A    Loss = 108 HE/t         ← within gold limit (25A barely), 0.2% loss
EV  (7680V)   I = 6.5A   Loss = 6.8 HE/t         ← within steel limit, 0.01% loss
SC (30720V)   I = 1.6A   Loss = 0.02 HE/t        ← near-zero loss
```

The natural conclusion: **long-distance power should be transmitted at the highest available voltage tier.**

### Optimal Transmission Voltage by Distance

| Distance | Recommended Voltage | Rationale |
|----------|-------------------|-----------|
| < 25m | LV or MV | Short run, cheap materials |
| 25-100m | MV or HV | Medium distance, moderate loss |
| 100-500m | HV or EV | Long trunk — step up/down |
| 500m+ | EV or SC | Dedicated transmission line |
| Any (insane power) | BUS | Dyson/DFC scale |

### Pylon Transformer Variant

Medium and Large pylons have a **transformer variant** that steps voltage up/down:

```
╔══════════════════════════════════════════════════════╗
║  [LV Cable] → Pylon Transformer → HV Pylon → wire   ║
║              → HV Pylon → Pylon Transformer → [LV]   ║
╚══════════════════════════════════════════════════════╝
```

- Input port at base connects to local cable network
- Steps voltage up to pylon's rated tier for the wire span
- Steps back down at the destination pylon
- Configurable throughput limit per direction
- Prevents lower-tier cables from being the bottleneck

### Wire Overload & Visual Feedback

| Condition | Visual | Mechanical |
|-----------|--------|------------|
| Current > 80% of max | Wires sag, occasional sparks | Warning particles |
| Current > 100% of max sustained 5s | Wire snaps, fire | Drops wire item, pylon catches fire |
| Recovery | — | Re-string with new wire spool |

### Connection Workflow

1. Place pylons at both ends (must be within range)
2. Hold wire spool (copper/gold/steel/etc.) in hand
3. Right-click first pylon with `ItemWiring` → sets start
4. Right-click second pylon → wire is strung, material consumed
5. Wire type determines Ω/m and max current for that link
6. Different wires can connect to the same multi-connection pylon (mix copper and gold on a medium pylon)

### Relationship to Other Transportation Methods

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         TRANSPORTATION OPTIONS                          │
├──────────────────┬──────────┬───────────┬──────────┬────────────────────┤
│ Method           │ Range    │ Voltage   │ Loss     │ Best For           │
├──────────────────┼──────────┼───────────┼──────────┼────────────────────┤
│ LV/MV cable      │ <25m     │ LV/MV     │ I²R/tile │ Machine networks   │
│ Connectors       │ <10m     │ LV        │ I²R×dist │ Wall endpoints     │
│ Small pylon      │ <25m     │ MV        │ I²R×dist │ Local distribution │
│ Medium pylon     │ <45m     │ HV        │ I²R×dist │ Inter-base         │
│ Large pylon      │ <100m    │ EV        │ I²R×dist │ Long trunk         │
│ Pylon transformer│ <100m    │ step-up   │ +5% conv │ Tier bridging      │
│ Bus bar          │ contact  │ ∞        │ 0        │ Endgame, insane P  │
│ Dyson beam       │ 24 blocks│ N/A      │ 0        │ Space→ground       │
│ DFC laser        │ 50 blocks│ N/A      │ 5%       │ Reactor coupling   │
└──────────────────┴──────────┴───────────┴──────────┴────────────────────┘
```

---

## Power Transfer Matrix

```
                    ┌─────────────────────────────────────────────────────┐
                    │                    RECEIVING SIDE                    │
                    │  LV     MV     HV     EV     SC     BUS    BEAM     │
┌───────┬───────────┼─────────────────────────────────────────────────────┤
│  LV   │  Cable    │  ✓     Trf     Trf     Trf    Trf    ✗      ✗      │
│  MV   │  Cable    │  Trf     ✓     Trf     Trf    Trf    ✗      ✗      │
│  HV   │  Cable    │  Trf    Trf      ✓     Trf    Trf    ✗      ✗      │
│  EV   │  Cable    │  Trf    Trf     Trf      ✓    Trf    ✗      ✗      │
│  SC   │  Cable    │  Trf    Trf     Trf     Trf     ✓    ✗      ✗      │
│  BUS  │  Contact  │  ✗      ✗      ✗       ✗     ✗      ✓      ✗      │
│ BEAM  │  LOS      │  ✗      ✗      ✗       ✗     ✗      ✓      ✓      │
└───────┴───────────┴─────────────────────────────────────────────────────┘

  ✓  = Direct connection
  Trf = Requires Transformer
  ✗   = Not possible
```

---

## Example: Full Base Power Flow

### Mid-game base with diesel generator

```
Diesel Generator (MV, 480V, 50K HE/t)
    │
    ├── MV Cable (max 10K HE/t) ── MV Battery
    │
    ├── MV Cable ── MV Electric Furnace (500 HE/t)
    │
    └── MV Cable ── MV Assembler (2K HE/t)
                   ── MV Centrifuge (3K HE/t)
                   ── MV Press (1K HE/t)

At 6.5K HE/t demand and 10K HE/t cable limit:
  I = 6500 / 480 = 13.5A
  Loss = 13.5² × 0.005 × cableCount = ~1 HE/t per cable
  All machines get 100% voltage → full efficiency
```

### End-game base with Dyson swarm

```
Dyson Receiver (beams power down)
    ↓
Dyson Converter HE (stores Long.MAX/tick, clears buffer every 10 ticks)
    ↓  beam-face contact
Bus Bar Segment ── Bus Bar Segment ── Bus Bar Segment ── Bus Bar Segment
    ↓                                                     ↓
BUS→HV Transformer (throttled to 90K HE/t)       BUS→SC Transformer (throttled to 50M HE/t)
    ↓                                                     ↓
HV Cable Network (max 100K HE/t)                  SC Superconductor Cable
    ↓                                                     ↓
HV Assemblers, HV Centrifuges                     EV Fusion MHDT input
    (total 85K HE/t)                                    (runs at 40M HE/t)
```

---

## Implementation Plan

### Phase 1 — Core API (`api/hbm/energymk3/`)

| File | Contents |
|------|----------|
| `VoltageTier.java` | Enum: LV/MV/HV/EV/SC/BUS with voltage values |
| `IEnergyConnectorMK3.java` | Marker + `getVoltageTier()` |
| `IEnergyConductorMK3.java` | `getMaxAmperage()`, `getResistance()`, `getMaxPower()`, `getInternalBuffer()` |
| `IEnergyHandlerMK3.java` | `getPower()/setPower()/getMaxPower()`, `getVoltageNominal()`, `getVoltageTolerance()` |
| `IEnergyReceiverMK3.java` | `transferPower()`, `trySubscribe()`, `getPriority()`, `onOvervoltage()`, `onUndervoltage()` |
| `IEnergyProviderMK3.java` | `usePower()`, `tryProvide()`, `getProviderSpeed()` |
| `PowerNetMK3.java` | Distribution algorithm with I²R loss, cable bottleneck, voltage matching |
| `NodespaceMK3.java` | UNINOS compatibility with `PowerNetProviderMK3` |

### Phase 2 — Cable Blocks

| Block | Tier | File |
|-------|------|------|
| CableCopper | LV | `blocks/network/CableCopper.java` |
| CableGold | MV | `blocks/network/CableGold.java` |
| CableSteel | HV | `blocks/network/CableSteel.java` |
| CableAdvanced | EV | `blocks/network/CableAdvanced.java` |
| CableSuperconductor | SC | `blocks/network/CableSuperconductor.java` |

Each cable has a `TileEntityCableMK3` that stores tier, resistance, maxAmperage, and internal buffer. Overload heat is tracked per-tick and synced to client for visual feedback.

### Phase 3 — Machines (First Wave)

| Machine | Tier | Type | Consumption/Production |
|---------|------|------|----------------------|
| Coal Generator | LV | Provider | 50 HE/t |
| Electric Furnace | LV | Consumer | 50 HE/t |
| Battery Box | LV | Storage | 100K HE |
| Gas Generator | MV | Provider | 1K HE/t |
| Assembler | MV | Consumer | 500 HE/t |
| Centrifuge | HV | Consumer | 5K HE/t |
| Combustion Engine | MV | Provider | 5K HE/t |
| Steam Turbine | HV | Provider | 20K HE/t |

### Phase 4 — Pylon System & Long-Distance Transport

| Block/Item | Tier | Notes |
|------------|------|-------|
| Wiring Tool MK3 | Generic | Updated `ItemWiring` with wire material selection |
| Copper Wire Spool | LV | Copper wire for pylon links |
| Gold Wire Spool | MV | Gold wire for pylon links |
| Steel Wire Spool | HV | Steel wire for pylon links |
| Superconductor Spool | SC | Zero-resistance wire |
| PylonConnector | LV | Wall-mount, 10m |
| PylonConnectorSuper | MV→HV | Wall-mount, 100m |
| PylonSmall | MV | 25m, SINGLE |
| PylonMedium | HV | 45m, TRIPLE |
| PylonMediumTransformer | HV | Step up/down variant |
| PylonLarge | EV→SC | 100m, QUAD |
| PylonLargeTransformer | EV→SC | Step up/down variant |
| Substation | HV→EV | Local hub, 20m, QUAD |

### Phase 5 — Transformers & Interop

| Block | Purpose |
|-------|---------|
| Transformer | Between any two voltage tiers |
| Pylon Transformer | Integrated step-up/down at pylon base |
| MK2→MK3 Bridge | Connects legacy MK2 to MK3 at configurable voltage |
| BUS Transformer | Between BUS and any cable tier |
| Cable Fuse | Resettable overload protection |

### Phase 6 — Bus Bar System

| Block | File |
|-------|------|
| Bus Bar Segment | `blocks/network/BusBar.java` |
| Bus Bar Corner | `blocks/network/BusBarCorner.java` |
| Bus Bar T-Junction | `blocks/network/BusBarTJunction.java` |

### Phase 7 — Endgame Content (MK3 versions)

Existing endgame machines get MK3-compatible variants where appropriate, but the beam/bus transfer pathways remain the primary method for endgame power transmission.

---

## Coexistence with MK2

- MK2 and MK3 use **separate UNINOS providers** — different node types, same `UniNodespace`
- They never interact directly
- Only the **MK2↔MK3 Bridge Transformer** connects them
- All existing MK2 `.java` files are **untouched**
- New machines implement MK3 interfaces alongside or instead of MK2
- `IEnergyHandlerMK2` and `IEnergyHandlerMK3` are separate hierarchies

---

## Constants Reference

| Parameter | Value | Notes |
|-----------|-------|-------|
| Cable heat threshold 1 (visual) | 80% of MaxPower | Cable turns orange |
| Cable heat threshold 2 (melt) | 120% of MaxPower sustained 5s | Drops item, fire |
| Fuse reset | Redstone pulse | Re-deploys cable |
| Transformer default loss | 5% | Configurable 0-25% |
| Overvoltage explosion radius multiplier | ceil(excess / V_nominal) | Per-block radius |
| Undervoltage minimum efficiency | 0.1 (10%) | Never fully zero |
| BUS max throughput | Long.MAX_VALUE | No practical limit |
| Cable internal buffer refill rate | MaxPower / 20 | Per tick |
| Wire overload visual threshold | 80% of MaxCurrent | Wires sag, sparks |
| Wire overload snap threshold | 100% of MaxCurrent sustained 5s | Drops item, fire |
| Pylon transformer conversion loss | 5% | Configurable |
| Minimum pylon height clearance | 2 blocks above | Preents ground-shorting |
| Wiring tool max connection attempts | 1 per click | Consumes wire spool on success |
| UNINOS MK3 provider ID | `THE_POWER_PROVIDER_MK3` | Separate from MK2 |
