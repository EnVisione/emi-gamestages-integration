# EMI GameStages Link

A Minecraft Forge mod that integrates [EMI](https://modrinth.com/mod/emi) with [GameStages](https://www.curseforge.com/minecraft/mc-mods/game-stages) to show visual lock indicators on items and recipes that are locked by stages.

## Features

- **Automatic ItemStages Integration**: Automatically detects items locked by [ItemStages](https://www.curseforge.com/minecraft/mc-mods/item-stages) - no config needed!
- **Automatic RecipeStages Integration**: Automatically detects recipes locked by [RecipeStages](https://www.curseforge.com/minecraft/mc-mods/recipe-stages) - no config needed!
- **Lock Icon Overlay**: Shows a lock icon on items that are locked by GameStages
- **"Locked to <stage>" Label**: Displays a label above locked recipes showing the required stage
- **Tooltip Info**: Shows which stage is required when hovering over locked items
- **Orange Highlight**: Highlights locked output slots in semi-transparent orange
- **Item List Integration**: Shows lock icons on locked items in EMI's search/item list panels
- **Manual Config Support**: Configure additional locks based on item IDs or tags

## Requirements

- Minecraft Forge 1.20.1 (47.x)
- EMI 1.0.0+
- GameStages 11.0.0+
- Bookshelf (required by GameStages)
- Java 17

### Optional (but recommended!)

- **ItemStages 8.0.0+**: For automatic item lock detection
- **RecipeStages 8.0.0+**: For automatic recipe lock detection

## Building

### Dependencies Setup

The mod uses CurseMaven and Modrinth Maven for dependencies. If you encounter dependency resolution errors:

#### Option 1: Update File IDs (Recommended)
Go to the mod pages on CurseForge and find the correct file IDs:
1. **GameStages**: https://www.curseforge.com/minecraft/mc-mods/game-stages/files - Click on the 1.20.1 version, the file ID is in the URL
2. **Bookshelf**: https://www.curseforge.com/minecraft/mc-mods/bookshelf/files - Click on the 1.20.1 version

Update the file IDs in `build.gradle`:
```groovy
compileOnly fg.deobf("curse.maven:game-stages-268655:YOUR_FILE_ID")
compileOnly fg.deobf("curse.maven:bookshelf-228525:YOUR_FILE_ID")
```

#### Option 2: Use Local Jars
1. Download the mod jars from CurseForge/Modrinth
2. Place them in the `libs/` folder
3. Update `build.gradle` to use:
```groovy
compileOnly fg.deobf(fileTree(dir: 'libs', include: ['*.jar']))
```

### Build Command
```bash
./gradlew build
```

The output jar will be in `build/libs/`

## Configuration

The mod creates a configuration file at `config/emi_gamestages_link-common.toml`.

### Display Settings

```toml
[display]
# Show 'Locked to <stage>' label above locked recipes
showLockLabel = true

# Show lock icon on locked items/outputs
showLockIcon = true

# Highlight locked recipe outputs in red
highlightLockedOutput = true

# Size of the lock icon in pixels (6-16)
iconSize = 8

# Padding from the edge of the slot for the lock icon
iconPadding = 1

# Show lock icon on items in EMI search/item list panels
showLockOnItemList = true

# Color for the locked output highlight (ARGB hex)
highlightColor = 0x80FF0000
```

### Lock Definitions

Add item or tag locks in the config:

```toml
[locks]
# Item locks in format 'modid:item_id=stage_name'
items = [
    "minecraft:diamond_pickaxe=diamond_age",
    "minecraft:netherite_sword=netherite_age"
]

# Tag locks in format '#modid:tag=stage_name'
tags = [
    "#forge:ingots/steel=steel_age",
    "#minecraft:logs=wood_age"
]
```

## How It Works

1. **Automatic Detection (ItemStages)**: If ItemStages is installed, the mod automatically queries ItemStages' restriction system to find locked items
2. **Automatic Detection (RecipeStages)**: If RecipeStages is installed, the mod automatically detects recipes that are stage-locked
3. **Config Fallback**: If the item isn't locked by ItemStages, checks the manual config for item/tag locks
4. **Player Stage Check**: Uses GameStages API to check if the player has the required stage
5. **Visual Indicators**: If locked, shows the lock icon, label, and/or red highlight

### Integration Settings

```toml
[integration]
# Enable automatic integration with ItemStages mod
enableItemStages = true

# Enable automatic integration with RecipeStages mod  
enableRecipeStages = true
```

You can disable automatic integrations if you prefer to use only manual config locks.

## Screenshots

When an item is locked:
- A lock icon appears at the top-left corner of the item slot (to avoid overlapping with item counts)
- The recipe display shows "🔒 Locked to: <stage_name>" at the top
- The output slot is highlighted in semi-transparent orange
- Hovering over the item shows "🔒 This item is locked" and "Stage required: <stage_name>" in the tooltip

## License

All Rights Reserved

## Credits

- Lock icon texture included as `PIXELLOCK.png`
- Built for integration with EMI by emi and GameStages by Darkhax

