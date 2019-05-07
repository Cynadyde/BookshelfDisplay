package me.cynadyde.bookshelves;

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

@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesPlugin extends JavaPlugin implements Listener, CommandExecutor {

    public static final String tag = Utils.format("&f[&9BookShelves&f]&r ");

    @Override
    public void onEnable() {

        // Register plugin commands...
        PluginCommand cmdReloadConfig = getCommand("bookshelves reloadconfig");
        if (cmdReloadConfig != null) {
            cmdReloadConfig.setExecutor(this);
            cmdReloadConfig.setPermissionMessage(tag + Utils.format("&cInsufficient permissions."));
        }
        // Register plugin event listeners...
        getServer().getPluginManager().registerEvents(this, this);

        // Reload the plugin config and refresh template data...
        saveDefaultConfig();
        reloadConfig();

        GuiTemplate.refreshDataFromConfig(getLogger(), getConfig());
    }

    @Override
    public void onDisable() {

        // Close any active GUIs...
        for (BookShelvesGUI gui : BookShelvesGUI.activeGUIs()) {
            gui.close();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        GuiTemplate.refreshDataFromConfig(getLogger(), getConfig());

        return true;
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
        // Cancel the interaction event...
        event.setCancelled(true);

        // If the bookshelf has a GUI, show it to the player...
        if (bookShelf.hasGUI()) {
            BookShelvesGUI.openGUI(event.getPlayer(), bookShelf);
        }
        // Otherwise, show the first book to the player...
        else {
            Utils.openBook(event.getPlayer(), bookShelf.getContents().get(0));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {

        Inventory inv = event.getClickedInventory();

        // An inventory was interacted with...
        if (inv == null) {
            return;
        }
        // The inventory is a bookshelf GUI...
        BookShelvesGUI gui = BookShelvesGUI.getGUI(inv);
        if (gui == null) {
            return;
        }
        // A player interacted with the inventory GUI...
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // Interact with the GUI and cancel the click event...
        gui.slotClicked(player, event.getSlot());
        event.setCancelled(true);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {

        BookShelvesGUI gui = BookShelvesGUI.getGUI(event.getInventory());
        if (gui != null) {
            gui.close();
        }
    }
}
