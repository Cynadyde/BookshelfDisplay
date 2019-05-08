package me.cynadyde.bookshelves;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesPlugin extends JavaPlugin implements Listener, CommandExecutor {

    PluginCommand mainCmd;

    /**
     * Formatted text to be sent to chat / console by the plugin.
     */
    public static class Msgs {

        public static final String tag = Utils.format("&f[&bBookShelves&f]&r ");

        public static String noPerms() {
            return tag + Utils.format("&cInsufficient permissions... ");
        }

        public static String unknownCmd(String alias, String...args) {
            return tag + Utils.format("&cUnknown command: &6/%s %s &c... ", alias, String.join(" ", args));
        }

        public static String badArgs(String usage) {
            return tag + Utils.format("&cUsage: &6%s &c...");
        }
    }

    @Override
    public void onEnable() {

        // Register plugin commands...
        mainCmd = getCommand("bookshelves");
        assert (mainCmd != null);

        mainCmd.setExecutor(this);

        // Register plugin event listeners...
        getServer().getPluginManager().registerEvents(this, this);

        // Reload the plugin config and refresh template data...
        saveDefaultConfig();
        reloadConfig();

        GuiStyle.refreshDataFromConfig(getLogger(), getConfig());
    }

    @Override
    public void onDisable() {

        // Close any active GUIs on plugin disable...
        for (BookShelvesGui gui : BookShelvesGui.activeGUIs()) {
            gui.close();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        // The '/bookshelves' command...
        if (!alias.equalsIgnoreCase("bookshelves")) {
            return false;
        }
        // Sender has player permissions...
        if (!sender.hasPermission("bookshelves.player")) {
            sender.sendMessage(Msgs.noPerms());
        }
        // No arguments given...
        if (args.length == 0) {

            List<String> message = new ArrayList<>();

            // Message header with plugin name & version...
            message.add(Utils.format("&e==--===--==&f{ &9&l%s &f}&e==--===--==", getDescription().getFullName()));

            // Plugin description...
            message.add(Utils.format("&6%s", getDescription().getDescription()));
            message.add(Utils.format("&e%s", getDescription().getWebsite()));

            // Plugin help...
            message.add(Utils.format("&aSimply attach an item-frame to a bookshelf block. " +
                    "You may put a book there or a container full of books."));

            // List of commands...
            message.add(Utils.format("&b/bookshelves templates [<page>]"));
            message.add(Utils.format("&7Display a list of GUI templates defined in the config. " +
                    "To use these, name a container and place it in the bookshelf's item frame."));

            message.add(Utils.format("&b/bookshelves reloadconfig"));
            message.add(Utils.format("&7Reload the plugin's configuration file."));
            message.add("");

            // Send the message...
            sender.sendMessage(message.toArray(new String[message.size()]));
            return true;
        }

        // The '/bookshelves templates' command...
        else if (args[0].equalsIgnoreCase("templates")) {

            String pageStr = "1";
            int page = 1;

            // A page argument was given...
            if (args.length == 2) {

                pageStr = args[1];
                try {
                    page = Integer.parseInt(pageStr);
                }
                catch (NumberFormatException ex) {
                    page = 0;
                }
            }
            // To many arguments were given...
            else if (args.length > 2) {
                sender.sendMessage(Msgs.badArgs("/bookshelves templates [<page>]"));
                return false;
            }

            // Get the list of templates...
            List<String> templates = GuiStyle.getStyles();

            // Determine start and end index...
            page -= 1;
            int totalItems = templates.size();
            int itemsPerPage = 12;
            int maxPage = (int) Math.ceil(totalItems / (double) itemsPerPage);
            int startIndex = (page * itemsPerPage);
            int endIndex = startIndex + itemsPerPage;

            List<String> message = new ArrayList<>();

            // Message header and page number...
            message.add(Utils.format("&e==--===--==&f{ &9&l%s &b&lTemplates &6pg &c%s/%d &f}&e==--===--==",
                    getDescription().getName(), pageStr, maxPage));

            // Add the page of template names to the message...
            for (int i = startIndex; i < endIndex; i++) {
                if (i < templates.size()) {
                    message.add(Utils.format("&7- &a%s", templates.get(i)));
                }
            }
            // Send the message...
            sender.sendMessage(new String[message.size()]);
            return true;
        }

        // The '/bookshelves reloadconfig' command...
        else if (args[0].equalsIgnoreCase("reloadconfig")) {

            // Sender has admin perms...
            if (!sender.hasPermission("bookshelves.admin")) {
                sender.sendMessage(Msgs.noPerms());
                return false;
            }

            // reload the config and refresh cached data...
            reloadConfig();
            GuiStyle.refreshDataFromConfig(getLogger(), getConfig());
            return true;
        }
        // Unknown command...
        else {
            sender.sendMessage(Msgs.unknownCmd(alias, args));
            return false;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {

        // The player has right-clicked...
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        // The player has permission to use plugin features...
        if (!event.getPlayer().hasPermission("bookshelves.player.*")) {
            return;
        }
        // The player is not sneaking...
        if (event.getPlayer().isSneaking()) {
            return;
        }
        // A block has been clicked...
        if (event.getClickedBlock() == null) {
            return;
        }
        // The block clicked is a functional bookshelf...
        BookShelvesBlock bookShelf = BookShelvesBlock.from(event.getClickedBlock());
        if (bookShelf == null) {
            return;
        }
        // The bookshelf has a root item...
        if (bookShelf.getRoot() == null) {
            return;
        }
        // Cancel the interaction event...
        event.setCancelled(true);

        // If auto-view is true and the root is a written book, open it for player...
        if (getConfig().getBoolean("auto-view", true)
                && bookShelf.getRoot().getType().equals(Material.WRITTEN_BOOK)) {
            Utils.openBook(event.getPlayer(), bookShelf.getRoot());
        }
        // Otherwise, open the bookshelf GUI for the player...
        else {
            BookShelvesGui.openGUI(bookShelf, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {

        Inventory inv = event.getClickedInventory();

        // An inventory was interacted with...
        if (inv == null) {
            return;
        }
        // A player interacted with the inventory...
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // The inventory is a bookshelf GUI...
        BookShelvesGui gui = BookShelvesGui.getGUI(inv);
        if (gui == null) {
            return;
        }
        // Cancel normal interaction...
        event.setCancelled(true);

        // Have the GUI handle the event...
        gui.getView().slotClicked(player, event.getSlot());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {

        // If the inventory was a bookshelf GUI, close the GUI...
        BookShelvesGui gui = BookShelvesGui.getGUI(event.getInventory());
        if (gui != null) {
            gui.close();
        }
    }
}
