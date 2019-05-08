package me.cynadyde.bookshelves;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main class of the BookShelves plugin.
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesPlugin extends JavaPlugin implements Listener, CommandExecutor {

    public String tag() {
        //noinspection ConstantConditions
        return Utils.format(getConfig().getString("messages.tag"));
    }

    public String noPermsMsg() {
        //noinspection ConstantConditions
        return tag() + Utils.format(getConfig().getString("messages.no-perms"));
    }

    public String unknownCmdMsg(String alias, String... args) {
        //noinspection ConstantConditions
        return tag() + Utils.format(getConfig().getString("messages.unknown-cmd"), alias, String.join(" ", args));
    }

    public String badArgsMsg(String usage) {
        //noinspection ConstantConditions
        return tag() + Utils.format(getConfig().getString("messages.bad-args"), usage);
    }

    @Override
    public void onEnable() {

        //noinspection ConstantConditions
        getCommand("bookshelves").setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        getConfig().setDefaults(defaultConfig());
        reloadConfig();

        GuiStyle.refreshDataFromConfig(getLogger(), getConfig());
    }

    @Override
    public void onDisable() {

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
        // No arguments given...
        if (args.length == 0) {

            return pluginHelpCmd(sender);
        }
        // The '/bookshelves styles' command...
        else if (args[0].equalsIgnoreCase("styles")) {

            return styleListCmd(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        // The '/bookshelves reloadconfig' command...
        else if (args[0].equalsIgnoreCase("reloadconfig")) {

            return reloadConfigCmd(sender);
        }
        // Unknown command...
        else {
            if (!sender.hasPermission("bookshelves.player")) {
                sender.sendMessage(noPermsMsg());
                return false;
            }
            sender.sendMessage(unknownCmdMsg(alias, args));
            return false;
        }
    }

    /**
     * Open bookshelf GUIs when bookshelf is clicked.
     */
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

    /**
     * Update bookshelf GUIs when inventory is clicked.
     */
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

    /**
     * Close bookshelf GUIs when inventory is closed.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {

        // If the inventory was a bookshelf GUI, close the GUI...
        BookShelvesGui gui = BookShelvesGui.getGUI(event.getInventory());
        if (gui != null) {
            gui.close();
        }
    }

    /**
     * Prints plugin description, help, and commands to the sender.
     */
    public boolean pluginHelpCmd(CommandSender sender) {

        if (!sender.hasPermission("bookshelves.player")) {
            sender.sendMessage(noPermsMsg());
            return false;
        }

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
        message.add(Utils.format("&b/bookshelves styles [<page>]"));
        message.add(Utils.format("&7Display a list of GUI templates defined in the config. " +
                "To use these, name a container and place it in the bookshelf's item frame."));

        message.add(Utils.format("&b/bookshelves reloadconfig"));
        message.add(Utils.format("&7Reload the plugin's configuration file."));
        message.add("");

        // Send the message...
        sender.sendMessage(message.toArray(new String[message.size()]));
        return true;
    }

    /**
     * Prints a list of GUI styles to the sender.
     */
    public boolean styleListCmd(CommandSender sender, String[] args) {

        if (!sender.hasPermission("bookshelves.player")) {
            sender.sendMessage(noPermsMsg());
            return false;
        }

        String pageStr = "1";
        int page = 1;

        // A page argument was given...
        if (args.length == 1) {

            pageStr = args[0];
            try {
                page = Integer.parseInt(pageStr);
            }
            catch (NumberFormatException ex) {
                page = 0;
            }
        }
        // To many arguments were given...
        else if (args.length > 1) {
            sender.sendMessage(badArgsMsg("/bookshelves styles [<page>]"));
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
        message.add(Utils.format("&e==--===--==&f{ &9&l%s &b&lStyles &6pg &c%s/%d &f}&e==--===--==",
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

    /**
     * Reloads the plugin's config & data.
     */
    public boolean reloadConfigCmd(CommandSender sender) {

        if (!sender.hasPermission("bookshelves.admin")) {
            sender.sendMessage(noPermsMsg());
            return false;
        }

        reloadConfig();
        GuiStyle.refreshDataFromConfig(getLogger(), getConfig());
        return true;
    }

    /**
     * Gets the default configuration from the plugin's jar.
     */
    public Configuration defaultConfig() {

        InputStream defaultConfig = getResource("config.yml");
        assert (defaultConfig != null);

        return YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfig));
    }

    /**
     * Makes sure the values in the config are usable, else the
     * config is renamed as broken and a new one is generated.
     */
    public void checkConfig() {

    }
}
