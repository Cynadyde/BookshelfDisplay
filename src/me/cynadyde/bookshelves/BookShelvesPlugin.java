package me.cynadyde.bookshelves;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the BookShelves plugin.
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {

        // Register the plugin's event listeners...
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

        // Close out any active guis on plugin disable...
        for (BookShelvesGui gui : BookShelvesGui.activeGuis()) {
            gui.getOwner().closeInventory();
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
        // The player is not sneaking...
        if (event.getPlayer().isSneaking()) {
            return;
        }
        // A block has been clicked...
        if (event.getClickedBlock() == null) {
            return;
        }
        // The player has permission to use plugin features...
        if (!event.getPlayer().hasPermission("bookshelves.*")) {
            return;
        }
        // The block clicked is a functional bookshelf...
        BookShelvesContainer bookShelf = BookShelvesContainer.at(event.getClickedBlock());
        if (bookShelf == null) {
            return;
        }
        // Cancel the interaction event...
        event.setCancelled(true);

        // Open the bookshelf gui for the player...
        BookShelvesGui.openGui(bookShelf, event.getPlayer());
    }

    /**
     * Update bookshelf GUIs when inventory is clicked.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {

        // The inventory was a bookshelf gui...
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        BookShelvesGui gui = BookShelvesGui.getActiveGui((Player) event.getWhoClicked());
        if (gui == null) {
            return;
        }
        // Cancel normal interaction...
        event.setCancelled(true);

        ItemStack cursor = event.getCursor();
        if (cursor == null) {
            cursor = new ItemStack(Material.AIR, 1);
        }

        // Have the gui handle the event...
        gui.onInteract(event.getWhoClicked(), event.getInventory(), event.getSlot(), event.getClick(), cursor);
    }

    /**
     * Close bookshelf GUIs when inventory is closed.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {

        BookShelvesGui.onInventoryClose(event);
    }
}
