# Mass

Minecraft should care what you are carrying.

Mass gives every item a weight. Carry too much and you slow down, lose sprint, lose jump, sink in water, and make horses regret meeting you.

## What it does

- Tracks weight across armor, hotbar, inventory, and offhand
- Slows movement as your load goes up
- Disables sprint and jump at higher weight tiers
- Makes heavy players sink in water
- Slows horses and counts chest cargo on horse-type mounts
- Blocks elytra when you are over the limit
- Adds the `Lightening` enchantment to make armor lighter

A diamond chestplate weighs 8 kg. Full diamond is enough to matter. Full netherite is a life choice.

## Install

- Use Paper `1.21.11+`
- Drop the jar into `plugins/`
- Start the server once
- Edit `plugins/Mass/config.yml` if you want different weights or penalty thresholds
- Run `/mass reload`

## Commands

All commands use `mass.admin`.

- `/mass reload`
- `/mass info [player]`
- `/mass lore [player]`
- `/mass item set <kg>`
- `/mass item clear`
- `/mass give lightening [1-3]`

## Config

The config is where most of the real control lives.

You can change:

- item weights
- category defaults
- sprint and jump thresholds
- water sinking
- horse slowdown
- elytra limit
- enchantment settings

If you want the plugin to feel punishing, you can do that. If you want it lighter and more survival-friendly, you can do that too.

## Build

```bash
mvn clean package
```

Output goes to `target/Mass-1.2.jar`.

## Notes

- Stackable items do not get lore or extra item data that would break stacking
- The plugin uses `Lightening`, not `Lightning`. That is on purpose
- If something feels off, check the config first. Most of the balance lives there

## Discord

If you want updates, feedback, or bug reports, join the Discord:

[discord.gg/WQ628mcr4w](https://discord.gg/WQ628mcr4w)
