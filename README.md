# Mass

A Paper plugin for Minecraft 1.21.11 that adds a weight and encumbrance system. Every item you carry has a physical weight that slows you down, limits jumping, and affects the things you ride.

## How it works

Every item has a weight in kg based on what it is made of. One diamond is 1 kg, so a diamond chestplate (8 diamonds) weighs 8 kg. Everything scales from there: iron is lighter, gold and netherite are heavier.

Weight is tracked across all inventory slots: armor, hotbar, main inventory, and offhand. As your total load increases you slow down and eventually cannot sprint or jump. Full diamond armor alone will disable sprinting. Full netherite is basically immovable.

Horses slow down based on the rider's weight. Donkeys with chests include the chest contents in the load calculation. Elytra gliding is blocked entirely once you are too heavy.

The Lightening enchantment can be applied to armor to reduce its contribution to your weight. It comes in three levels (30%, 45%, 60% reduction) and can be obtained from enchanting tables, librarian villagers, loot chests, or with `/mass give lightening`.

## Building

Requires Java 21 and Maven.

```
mvn package
```

The compiled jar ends up at `target/Mass-#.#.jar`.

## Commands

All commands require the `mass.admin` permission.

| Command | Description |
|---|---|
| `/mass reload` | Reload config.yml |
| `/mass info [player]` | Show weight breakdown |
| `/mass lore [player]` | Force-refresh weight lore in inventory |
| `/mass item set <kg>` | Override held item's weight |
| `/mass item clear` | Remove weight override from held item |
| `/mass give lightening [1-3]` | Give a Lightening enchanted book |

## Configuration

Edit `plugins/Mass/config.yml`. You can change individual item weights, penalty thresholds, vehicle settings, and enchantment behavior. Run `/mass reload` to apply changes without restarting.
