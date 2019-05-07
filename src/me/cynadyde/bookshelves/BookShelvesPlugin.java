package me.cynadyde.bookshelves;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesPlugin extends JavaPlugin implements Listener {

    // TODO define default gui template in config (including # rows per page)

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

        for (BookShelvesGUI gui : BookShelvesGUI.activeGUIs()) {
            gui.close();
        }
    }


    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {

        // The player has right-clicked a block...
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        // The player has permission to use plugin features...
        if (!event.getPlayer().hasPermission("bookshelves.*")) {
            return;
        }
        // The player is not sneaking...
        if (event.getPlayer().isSneaking()) {
            return;
        }
        // The block clicked is a functional bookshelf...
        BookShelvesBlock bookShelf =  BookShelvesBlock.from(event.getClickedBlock());
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
            openBook(event.getPlayer(), bookShelf.getContents().get(0));
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

        // An item in the inventory gui was clicked...
        ItemStack clickedItem = event.getInventory().getItem(event.getSlot());
        if (clickedItem == null) {
            return;
        }

        // Player clicked the backwards navigation button in the GUI...
        if (event.getSlot() == BookShelvesGUI.backButtonIndex &&
                clickedItem.getType().equals(BookShelvesGUI.backButton.getType())) {
            gui.updateDisplay(-1);
        }

        // Player clicked the forwards navigation button in the GUI...
        else if (event.getSlot() == BookShelvesGUI.nextButtonIndex &&
                clickedItem.getType().equals(BookShelvesGUI.forwardButton.getType())) {
            gui.updateDisplay(1);
        }

        // Player selected a book to read in the GUI...
        else if (clickedItem.getType().equals(Material.WRITTEN_BOOK)) {
            player.closeInventory();
            openBook(player, clickedItem);
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {

        BookShelvesGUI gui = BookShelvesGUI.getGUI(event.getInventory());
        if (gui != null) { gui.close(); }
    }

    /* have the player read a book that they do not possess */
    public void openBook(Player player, ItemStack book) {

        int slot = player.getInventory().getHeldItemSlot();
        ItemStack old = player.getInventory().getItem(slot);

        try {
            ByteBuf buffer = Unpooled.buffer(256);
            buffer.setByte(0, (byte) 0);
            buffer.writerIndex(1);

            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CUSTOM_PAYLOAD);
            packet.getModifier().writeDefaults();
            packet.getModifier().write(1, MinecraftReflection.getPacketDataSerializer(buffer));
            packet.getStrings().write(0, "MC|BOpen");

            player.getInventory().setItem(slot, book);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
        catch (Exception ex) {

            getLogger().log(Level.SEVERE, "Unable to open book for " + player.getName(), ex);
        }

        player.getInventory().setItem(slot, old);
    }
}
