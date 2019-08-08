package me.cynadyde.bookshelfdisplay;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the BookshelfDisplay plugin.
 */
public class BookshelfDisplayPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {

        // Register the plugin's event listeners...
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

        // Close out any active guis on plugin disable...
        for (BookshelfDisplayGui gui : BookshelfDisplayGui.activeGuis()) {
            gui.getOwner().closeInventory();
        }
    }

    /**
     * Open bookshelf GUIs when a bookshelf is clicked.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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
        // The player has permission to view a bookshelf display...
        if (!event.getPlayer().hasPermission("bookshelfdisplay.view")) {
            return;
        }
        // The block clicked is a functional bookshelf...
        BookshelfDisplayContainer bookShelf = BookshelfDisplayContainer.at(event.getClickedBlock());
        if (bookShelf == null) {
            return;
        }
        // Cancel the interaction event...
        event.setCancelled(true);

        // Create particle effect...
        Location particleLoc = event.getClickedBlock().getLocation()
                .add(0.5f, 0.5f, 0.5f).add(event.getBlockFace().getDirection().multiply(0.3));

        Object particleData = Material.BOOKSHELF.createBlockData();
        event.getPlayer().getWorld().spawnParticle(Particle.BLOCK_DUST, particleLoc, 6, particleData);

        // Open the bookshelf gui for the player...
        BookshelfDisplayGui.openGui(bookShelf, event.getPlayer());
    }

    /**
     * Update bookshelf GUIs when an inventory is clicked.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {

        // The clicker was a player...
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // The player is viewing a bookshelf gui...
        BookshelfDisplayGui gui = BookshelfDisplayGui.getActiveGui((Player) event.getWhoClicked());
        if (gui == null) {
            return;
        }

        // Cancel dragging on the gui inventory...
        if (event.getInventory() == gui.getInventory()) {
            event.setCancelled(true);
        }
    }

    /**
     * Update bookshelf GUIs when an inventory is clicked.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {

        // The clicker was a player...
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // The player is viewing a bookshelf gui...
        BookshelfDisplayGui gui = BookshelfDisplayGui.getActiveGui((Player) event.getWhoClicked());
        if (gui == null) {
            return;
        }

        // Cancel clicking on the gui inventory...
        if (event.getClickedInventory() == gui.getInventory()) {
            event.setCancelled(true);
        }

        // Have the gui handle the event...
        gui.onInteract(event.getClickedInventory(), event.getSlot(), event.getClick(), event.getCursor());
    }

    /**
     * Close bookshelf GUIs when an inventory is closed.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {

        BookshelfDisplayGui.onInventoryClose(event);
    }
}
