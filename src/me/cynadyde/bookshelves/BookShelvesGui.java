package me.cynadyde.bookshelves;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    public static BookShelvesGui openGui(@NotNull BookShelvesContainer bookshelf, @NotNull Player player, @NotNull List<PathIndex> path) {

        player.closeInventory();

        BookShelvesGui gui = new BookShelvesGui(bookshelf, player, path);

        player.openInventory(gui.getInventory());
        activeGUIs.add(gui);

        // OPEN BOOKSHELF SFX...
        gui.owner.playSound(gui.owner.getLocation(), Sound.ITEM_BOOK_PUT, 2.0f, 1.50f);

        return gui;
    }

    /**
     * Opens the gui for a bookshelf with the given player.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static BookShelvesGui openGui(@NotNull BookShelvesContainer bookshelf, @NotNull Player player) {

        return openGui(bookshelf, player, new ArrayList<>());
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
    private static ItemStack fillItem = Utils.itemStack(1, Material.GRAY_STAINED_GLASS_PANE, " ");
    private static ItemStack prevBtnItem = Utils.itemStack(1, Material.RED_STAINED_GLASS_PANE, Utils.format("&4&lPrevious Shelf"));
    private static ItemStack nextBtnItem = Utils.itemStack(1, Material.GREEN_STAINED_GLASS_PANE, Utils.format("&2&lNext Shelf"));

    private BookShelvesContainer bookshelf;
    private Player owner;
    private Inventory inventory;
    private List<PathIndex> path;
    private boolean interactive;

    private BookShelvesGui(@NotNull BookShelvesContainer bookshelf, @NotNull Player owner, @NotNull List<PathIndex> path) {

        this.owner = owner;
        this.bookshelf = bookshelf;
        this.path = path;

        if (path.isEmpty()) {
            path.add(new PathIndex(bookshelf.getRoot(), 0));
        }
        // Get the path name for the inventory...
        String name = path.get(path.size() - 1).dir.getCustomName();
        if (name == null) {
            name = "Bookshelf";
        }
        this.inventory = Bukkit.createInventory(owner, 45, name);
        this.interactive = true;

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
    public @NotNull List<PathIndex> getPath() {
        return Collections.unmodifiableList(path);
    }

    /**
     * Player clicks the inventory gui to read books or change directories.
     */
    public void onInteract(@Nullable Inventory inv, int slot, @NotNull ClickType click, @NotNull ItemStack cursor) {

        // Inside the inventory gui...
        if (!inventory.equals(inv)) {
            return;
        }

        // Left click with an empty cursor...
        if (interactive && click.isLeftClick() && cursor.getType().equals(Material.AIR)) {

            ItemStack clicked = inventory.getItem(slot);
            if (clicked != null) {

                // If the item is a book, close the Gui and open the book...
                if (clicked.getType().equals(Material.WRITTEN_BOOK)) {

                    // OPEN BOOK SFX...
                    owner.playSound(owner.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.5f, 0.67f);

                    owner.closeInventory();
                    Utils.openBook(owner, clicked);
                    waitForBookPutAway(owner.getLocation().clone());
                }
                // If the item is the prev-button item, go back a page...
                else if (clicked.equals(prevBtnItem)) {

                    path.get(path.size() - 1).start -= 27;
                    updateDisplay();
                }
                // If the item is the next-button item, go forward a page...
                else if (clicked.equals(nextBtnItem)) {

                    path.get(path.size() - 1).start += 27;
                    updateDisplay();
                }
                // If the item is the trim item, go up a directory...
                else if (clicked.equals(trimItem)) {

                    if (path.size() > 1) {

                        Container lastDir = path.remove(path.size() - 1).dir;
                        Container curDir = path.get(path.size() - 1).dir;

                        if (Utils.containersSimilar(lastDir, curDir)) {

                            animateDirectoryChange();
                        }
                        else {
                            animateDirectoryChangeNewInv();
                        }

                        // UP DIRECTORY SFX...
                        owner.playSound(owner.getLocation(), Sound.BLOCK_BARREL_CLOSE, 0.5f, 0.75f);
                    }
                }
                // If the item is a container, go into that directory...
                else {
                    Container container = Utils.getContainer(clicked);
                    if (container != null) {

                        Container lastDir = path.get(path.size() - 1).dir;
                        path.add(new PathIndex(container, 0));

                        if (Utils.containersSimilar(lastDir, container)) {

                            animateDirectoryChange();
                        }
                        else {
                            animateDirectoryChangeNewInv();
                        }

                        // DOWN DIRECTORY SFX...
                        owner.playSound(owner.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.5f, 0.75f);
                    }
                }
            }
        }
    }

    /**
     * Set the contents seen in the inventory gui.
     */
    public void updateDisplay() {

        if (path.isEmpty()) {
            path.add(new PathIndex(bookshelf.getRoot(), 0));
        }

        ItemStack[] contents = path.get(path.size() - 1).dir.getInventory().getStorageContents();
        int start = path.get(path.size() - 1).start;

        // Set the top trim...
        int ii = 0;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(ii, trimItem);
            ii++;
        }
        // Set the content area...
        for (int i = start; i < start + 27; i++) {
            if (i >= contents.length) {
                inventory.setItem(ii, fillItem);
            }
            else {
                inventory.setItem(ii, contents[i]);
            }
            ii++;
        }
        // Set the prev-button item....
        inventory.setItem(ii++, (start > 0) ? prevBtnItem : trimItem);

        // Set the bottom trim...
        for (int i = 1; i < 8; i++) {
            inventory.setItem(ii, trimItem);
            ii++;
        }
        // set the next-button item...
        inventory.setItem(ii, (start < contents.length - 1 - 27) ? nextBtnItem : trimItem);

        owner.updateInventory();
    }

    /**
     * Create a transitioning effect in the inventory gui.
     */
    private void animateDirectoryChange() {

        // Get a reference to the plugin...
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BookShelves");
        assert (plugin != null);

        interactive = false;

        // Fill the inventory with trim momentarily...
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, trimItem);
            inventory.setItem(i, trimItem);
        }
        owner.updateInventory();

        // Refresh the inventory after a half second...
        new BukkitRunnable() {
            @Override public void run() {
                updateDisplay();
                interactive = true;
            }
        }.runTaskLater(plugin, 10L);
    }

    /**
     * Create a transitioning effect in the inventory gui with a new inventory.
     * Has the unfortunate side-effect of resetting the mouse position.
     */
    private void animateDirectoryChangeNewInv() {

        // Get a reference to the plugin...
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BookShelves");
        assert (plugin != null);

        BookShelvesGui gui = new BookShelvesGui(bookshelf, owner, path);

        gui.interactive = false;

        // Fill the inventory with trim momentarily...
        for (int i = 0; i < gui.inventory.getSize(); i++) {
            inventory.setItem(i, trimItem);
            gui.inventory.setItem(i, trimItem);
        }
        gui.owner.updateInventory();

        // Refresh the inventory after a half second...
        new BukkitRunnable() {
            @Override public void run() {
                gui.updateDisplay();
                gui.interactive = true;
            }
        }.runTaskLater(plugin, 10L);

        owner.closeInventory();
        activeGUIs.add(gui);
        owner.openInventory(gui.getInventory());
    }

    /**
     * Wait for the player to move to open the gui again.
     */
    private void waitForBookPutAway(Location loc) {

        // Get a reference to the plugin...
        Plugin plugin = Bukkit.getPluginManager().getPlugin("BookShelves");
        assert (plugin != null);

        // Open the Gui again when the player moves at all...
        new BukkitRunnable() {
            @Override public void run() {

                if (!owner.getLocation().equals(loc)) {
                    openGui(bookshelf, owner, path);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    /**
     * The container and start index for the content of a gui.
     */
    public class PathIndex {

        public @NotNull Container dir;
        public @NotNull Integer start;

        public PathIndex(@NotNull Container container, @NotNull Integer index) {
            this.dir = container;
            this.start = index;
        }
    }
}
