package com.example.mass.command;

import com.example.mass.MassPlugin;
import com.example.mass.enchant.LighteningEnchant;
import com.example.mass.service.EncumbranceService;
import com.example.mass.service.LoreService;
import com.example.mass.service.WeightService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MassCommand extends Command {

    private static final String PERM = "mass.admin";

    private final MassPlugin         plugin;
    private final WeightService      weights;
    private final EncumbranceService encumbrance;
    private final LoreService        lore;

    public MassCommand(MassPlugin plugin, WeightService weights,
                       EncumbranceService encumbrance, LoreService lore) {
        super("mass", "Mass plugin admin commands.", "/mass <reload|info|lore|item|give>", List.of("m"));
        setPermission(PERM);
        this.plugin      = plugin;
        this.weights     = weights;
        this.encumbrance = encumbrance;
        this.lore        = lore;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(red("No permission."));
            return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        return switch (args[0].toLowerCase()) {
            case "reload"          -> cmdReload(sender);
            case "info", "weight"  -> cmdInfo(sender, args);
            case "lore"            -> cmdLore(sender, args);
            case "item"            -> cmdItem(sender, args);
            case "give"            -> cmdGive(sender, args);
            default                -> { sendHelp(sender); yield true; }
        };
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                             @NotNull String alias,
                                             @NotNull String[] args) {
        if (!sender.hasPermission(PERM)) return Collections.emptyList();

        return switch (args.length) {
            case 1 -> filter(List.of("reload", "info", "lore", "item", "give"), args[0]);
            case 2 -> switch (args[0].toLowerCase()) {
                case "info", "weight", "lore" -> filterPlayers(args[1]);
                case "item"                   -> filter(List.of("set", "clear"), args[1]);
                case "give"                   -> filter(List.of("lightening"), args[1]);
                default                       -> Collections.emptyList();
            };
            case 3 -> switch (args[0].toLowerCase()) {
                case "item" -> args[1].equalsIgnoreCase("set")
                        ? suggestWeights(sender)
                        : Collections.emptyList();
                case "give" -> args[1].equalsIgnoreCase("lightening")
                        ? filter(List.of("1", "2", "3"), args[2])
                        : Collections.emptyList();
                default -> Collections.emptyList();
            };
            default -> Collections.emptyList();
        };
    }

    private boolean cmdReload(@NotNull CommandSender sender) {
        plugin.reload();
        sender.sendMessage(green("Config reloaded."));
        return true;
    }

    private boolean cmdInfo(@NotNull CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 1);
        if (target == null) return true;

        double total     = weights.computeTotalWeight(target);
        double armor     = weights.computeArmorWeight(target);
        double hand      = weights.computeHandWeight(target);
        double inventory = weights.computeInventoryWeight(target);

        sender.sendMessage(Component.text("── Weight: " + target.getName() + " ──").color(NamedTextColor.GOLD));
        sender.sendMessage(stat("  Total",     fmt(total)));
        sender.sendMessage(stat("  Armour",    fmt(armor)));
        sender.sendMessage(stat("  Hands",     fmt(hand)));
        sender.sendMessage(stat("  Inventory", fmt(inventory) + " (includes hotbar)"));
        if (encumbrance.isSprintDisabled(target))
            sender.sendMessage(Component.text("  ⚠ Sprint disabled").color(NamedTextColor.RED));
        if (encumbrance.isJumpDisabled(target))
            sender.sendMessage(Component.text("  ⚠ Jumping disabled").color(NamedTextColor.RED));
        return true;
    }

    private boolean cmdLore(@NotNull CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 1);
        if (target == null) return true;

        ItemStack[] contents = target.getInventory().getStorageContents();
        for (ItemStack item : contents) if (item != null) lore.updateLore(item);
        target.getInventory().setStorageContents(contents);

        ItemStack[] armour = target.getInventory().getArmorContents();
        for (ItemStack piece : armour) if (piece != null) lore.updateLore(piece);
        target.getInventory().setArmorContents(armour);

        ItemStack off = target.getInventory().getItemInOffHand();
        lore.updateLore(off);
        target.getInventory().setItemInOffHand(off);

        sender.sendMessage(green("Refreshed weight lore for " + target.getName() + "."));
        return true;
    }

    private boolean cmdItem(@NotNull CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(red("Must be a player."));
            return true;
        }
        if (args.length < 2) { sendItemHelp(sender); return true; }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == org.bukkit.Material.AIR) {
            sender.sendMessage(red("Hold an item first."));
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "set"   -> cmdItemSet(player, held, args);
            case "clear" -> cmdItemClear(player, held);
            default      -> { sendItemHelp(sender); yield true; }
        };
    }

    private boolean cmdItemSet(@NotNull Player player, @NotNull ItemStack held, String[] args) {
        if (args.length < 3) {
            player.sendMessage(yellow("Usage: /mass item set <weight>"));
            return true;
        }
        double value;
        try { value = Double.parseDouble(args[2]); }
        catch (NumberFormatException ex) {
            player.sendMessage(red("Not a number: " + args[2]));
            return true;
        }
        if (value < 0) { player.sendMessage(red("Weight must be ≥ 0.")); return true; }
        weights.setWeight(held, value);
        lore.updateLore(held);
        player.getInventory().setItemInMainHand(held);
        encumbrance.scheduleUpdate(player);
        player.sendMessage(green("Set " + held.getType() + " weight → " + fmt(value) + "."));
        return true;
    }

    private boolean cmdItemClear(@NotNull Player player, @NotNull ItemStack held) {
        weights.clearWeight(held);
        lore.updateLore(held);
        player.getInventory().setItemInMainHand(held);
        encumbrance.scheduleUpdate(player);
        player.sendMessage(green("Cleared weight override from " + held.getType() + "."));
        return true;
    }

    private boolean cmdGive(@NotNull CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(red("Must be a player."));
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("lightening")) {
            sender.sendMessage(yellow("Usage: /mass give lightening [1|2|3]"));
            return true;
        }
        int level = 1;
        if (args.length >= 3) {
            try { level = Math.max(1, Math.min(3, Integer.parseInt(args[2]))); }
            catch (NumberFormatException ignored) {}
        }
        Enchantment lightening = LighteningEnchant.get();
        if (lightening == null) {
            sender.sendMessage(red("Lightening enchantment is not registered. Check server logs."));
            return true;
        }
        ItemStack book = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(lightening, level, false);
            book.setItemMeta(meta);
        }
        player.getInventory().addItem(book);
        player.sendMessage(green("Given Lightening " + level + " book."));
        return true;
    }

    private List<String> suggestWeights(@NotNull CommandSender sender) {
        List<String> list = new java.util.ArrayList<>(List.of("0.1", "0.5", "1.0", "2.0", "5.0", "10.0"));
        if (sender instanceof Player player) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() != org.bukkit.Material.AIR)
                list.add(0, String.format("%.2f", weights.getBaseWeight(held)));
        }
        return list;
    }

    private @Nullable Player resolvePlayer(@NotNull CommandSender sender, String[] args, int idx) {
        if (args.length > idx) {
            Player p = plugin.getServer().getPlayer(args[idx]);
            if (p == null) { sender.sendMessage(red("Player not found: " + args[idx])); return null; }
            return p;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage(yellow("Usage: /mass " + args[0] + " <player>"));
        return null;
    }

    private static String fmt(double v) { return String.format("%.2f kg", v); }

    private static Component stat(String label, String value) {
        return Component.text(label + ": ").color(NamedTextColor.GRAY)
                .append(Component.text(value).color(NamedTextColor.AQUA));
    }

    private static Component red(String msg)    { return Component.text(msg).color(NamedTextColor.RED); }
    private static Component green(String msg)  { return Component.text(msg).color(NamedTextColor.GREEN); }
    private static Component yellow(String msg) { return Component.text(msg).color(NamedTextColor.YELLOW); }

    private static void sendHelp(CommandSender s) {
        s.sendMessage(Component.text("Mass commands:").color(NamedTextColor.GOLD));
        s.sendMessage(yellow("  /mass reload"));
        s.sendMessage(yellow("  /mass info [player]           — weight breakdown"));
        s.sendMessage(yellow("  /mass lore [player]           — refresh weight lore"));
        s.sendMessage(yellow("  /mass item set <value>        — set held item weight"));
        s.sendMessage(yellow("  /mass item clear              — clear held item override"));
        s.sendMessage(yellow("  /mass give lightening [1|2|3] — give Lightening book"));
    }

    private static void sendItemHelp(CommandSender s) {
        s.sendMessage(yellow("Usage: /mass item <set <value>|clear>"));
    }

    private static List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filterPlayers(String prefix) {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
