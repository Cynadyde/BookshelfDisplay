package me.cynadyde.bookshelves;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Display the contents of a bookshelf to a player through an inventory GUI.
 */
@SuppressWarnings({ "WeakerAccess" })
public class BookShelvesGui {

    private static final Set<BookShelvesGui> activeGUIs = new HashSet<>();

    /**
     * Gets a set of all guis that are currently open.
     */
    public static @NotNull Set<BookShelvesGui> activeGuis() {
        return Collections.unmodifiableSet(activeGUIs);
    }

    /**
     * Fetches an opened gui with a known inventory.
     */
    public static @Nullable BookShelvesGui getActiveGui(@Nullable Inventory inventory) {

        if (inventory != null) {
            for (BookShelvesGui gui : activeGUIs) {
                if (gui.getInventory().equals(inventory)) {
                    return gui;
                }
            }
        }
        return null;
    }

    /**
     * Fetches an opened gui with a known bookshelf anchor.
     */
    @SuppressWarnings("unused")
    public static @Nullable BookShelvesGui getActiveGui(@Nullable Block anchor) {

        if (anchor != null) {
            for (BookShelvesGui gui : activeGUIs) {
                if (gui.getBookshelf().getAnchor().equals(anchor)) {
                    return gui;
                }
            }
        }
        return null;
    }

    /**
     * Fetches an opened gui with a known owner.
     */
    public static @Nullable BookShelvesGui getActiveGui(@Nullable Player owner) {

        if (owner != null) {
            for (BookShelvesGui gui : activeGUIs) {
                if (gui.getOwner().equals(owner)) {
                    return gui;
                }
            }
        }
        return null;
    }

    /**
     * Opens the gui for a bookshelf with the given player.
     */
    public static void openGui(@NotNull BookShelvesContainer bookshelf, @NotNull Player player) {
        BookShelvesGui gui = new BookShelvesGui(bookshelf, player);
        player.openInventory(gui.getInventory());
        activeGUIs.add(gui);

        // OPEN BOOKSHELF SFX...
        gui.owner.playSound(gui.owner.getLocation(), Sound.ITEM_BOOK_PUT, 2.0f, 1.50f);
    }

    /**
     * Handles inventory close events to shut down closed guis.
     */
    public static void onInventoryClose(@NotNull InventoryCloseEvent event) {

        BookShelvesGui gui = getActiveGui(event.getInventory());
        if (gui != null) {
            gui.inventory.clear();
            activeGUIs.remove(gui);

            // CLOSE BOOKSHELF SFX...
            gui.owner.playSound(gui.owner.getLocation(), Sound.ITEM_BOOK_PUT, 2.0f, 1.25f);
        }
    }

    private static ItemStack trimItem = Utils.itemStack(1, Material.BLACK_STAINED_GLASS_PANE, " ");

    private BookShelvesContainer bookshelf;
    private Player owner;
    private Inventory inventory;
    private List<Container> path;

    private BookShelvesGui(@NotNull BookShelvesContainer bookshelf, @NotNull Player owner) {

        this.owner = owner;
        this.bookshelf = bookshelf;
        this.path = new ArrayList<>();
        this.inventory = Bukkit.createInventory(owner, (9 * 5), bookshelf.getName());

        path.add(bookshelf.getRoot());

        updateDisplay();
    }

    public @NotNull BookShelvesContainer getBookshelf() {
        return bookshelf;
    }

    public @NotNull Player getOwner() {
        return owner;
    }

    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @SuppressWarnings("unused")
    public @NotNull List<Container> getPath() {
        return Collections.unmodifiableList(path);
    }

    public void onInteract(@Nullable Inventory inv, int slot, @NotNull ClickType click, @NotNull ItemStack cursor) {

        // Inside the inventory gui...
        if (inventory.equals(inv)) {

            // Left click with an empty cursor...
            if (click.isLeftClick() && cursor.getType().equals(Material.AIR)) {

                ItemStack clicked = inventory.getItem(slot);
                if (clicked != null) {

                    // If the item is a book, close the Gui and open the book...
                    if (clicked.getType().equals(Material.WRITTEN_BOOK)) {

                        // OPEN BOOK SFX...
                        owner.playSound(owner.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.5f, 0.67f);


                        owner.closeInventory();
                        Utils.openBook(owner, clicked);
                    }
                    // If the item is the trim item, go up a directory...
                    else if (clicked.equals(trimItem)) {
                        Bukkit.getLogger().info(Utils.format("Trying to go up a directory..."));

                        if (path.size() > 1) {
                            path.remove(path.size() - 1);
                            animateDirectoryChange();

                            // UP DIRECTORY SFX...
                            owner.playSound(owner.getLocation(), Sound.BLOCK_BARREL_CLOSE, 0.5f, 0.75f);
                        }
                    }
                    // If the item is a container, go into that directory...
                    else {
                        Container container = Utils.getContainer(clicked);
                        if (container != null) {
                            path.add(container);
                            animateDirectoryChange();

                            // DOWN DIRECTORY SFX...
                            owner.playSound(owner.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.5f, 0.75f);
                        }
                    }
                }
            }
        }
    }

    public void updateDisplay() {

        Bukkit.getLogger().info(Utils.format("Updating display..."));

        ItemStack[] contents = path.get(path.size() - 1).getInventory().getStorageContents();
        int ii = 0;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(ii, trimItem);
            ii++;
        }
        for (int i = 0; i < 27; i++) {
            if (i >= contents.length) {
                break;
            }
            inventory.setItem(ii, contents[i]);
            ii++;
        }
        for (int i = 0; i < 9; i++) {
            inventory.setItem(ii, trimItem);
            ii++;
        }
        owner.updateInventory();
    }

    public void animateDirectoryChange() {

        // Get a reference to the plugin...
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BookShelves");
        if (plugin == null) {
            updateDisplay();
            return;
        }
        // Fill the inventory with trim momentarily...
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, trimItem);
        }
        owner.updateInventory();

        // Refresh the inventory after a quarter second...
        new BukkitRunnable() {
            @Override public void run() {
                updateDisplay();
            }
        }.runTaskLater(plugin, 10L);
    }
}
