package me.cynadyde.bookshelves;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Display the contents of a bookshelf to a player through an inventory GUI.
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
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
    }

    /**
     * Handles inventory close events to shut down closed guis.
     */
    public static void onInventoryClose(@NotNull InventoryCloseEvent event) {

        BookShelvesGui gui = getActiveGui(event.getInventory());
        if (gui != null) {
            gui.inventory.clear();
            activeGUIs.remove(gui);
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

    public @NotNull List<Container> getPath() {
        return Collections.unmodifiableList(path);
    }

    public void onInteract(@NotNull HumanEntity who, @Nullable Inventory inv, int slot, @NotNull ClickType click, @NotNull ItemStack cursor) {

        // Click outside of the inventory gui to go up a directory...
        if (inv == null) {

            Bukkit.getLogger().info(Utils.format("Inv is null. going up a dir..."));

            if (path.size() > 1) {
                path.remove(path.size() - 1);
                updateDisplay();
            }
        }
        // Inside the inventory gui...
        else if (inventory.equals(inv)) {

            Bukkit.getLogger().info(Utils.format("Inv matches gui!"));
            Bukkit.getLogger().info(Utils.format("click: %s cursor: %s", click, cursor));

            // Left click with an empty cursor...
            if (click.isLeftClick() && cursor.getType().equals(Material.AIR)) {

                Bukkit.getLogger().info(Utils.format("Left clicking with empty cursor..."));

                ItemStack clicked = inventory.getItem(slot);
                if (clicked != null) {

                    Bukkit.getLogger().info(Utils.format("Clicked item was not null..."));

                    // If the item is a book, close the Gui and open the book...
                    if (clicked.getType().equals(Material.WRITTEN_BOOK)) {

                        Bukkit.getLogger().info(Utils.format("Clicked item was book! Opening book..."));

                        owner.closeInventory();
                        Utils.openBook(owner, clicked);
                    }
                    // If the item is a container, go into that directory...
                    else {
                        Container container = Utils.getContainer(clicked);
                        if (container != null) {

                            Bukkit.getLogger().info(Utils.format("Clicked item was contained..."));

                            path.add(container);
                            updateDisplay();
                        }
                        else {
                            Bukkit.getLogger().info(Utils.format("Clicked item was nothing functional."));
                        }
                    }
                }
            }
        }
        else {

            Bukkit.getLogger().info(Utils.format("Event inv did not match gui inv!?!?!"));
        }
    }

    public void updateDisplay() {

        Bukkit.getLogger().info(Utils.format("Updating display..."));

        ItemStack[] background = new ItemStack[inventory.getSize()];
        ItemStack[] contents = path.get(path.size() - 1).getInventory().getStorageContents();
        int ii = 0;
        for (int i = 0; i < 9; i++) {
            background[ii] = trimItem;
            ii++;
        }
        for (int i = 0; i < 27; i++) {
            if (i >= contents.length) {
                break;
            }
            background[ii] = contents[i];
            ii++;
        }
        for (int i = 0; i < 9; i++) {
            background[ii] = trimItem;
            ii++;
        }
        inventory.setContents(background);
        owner.updateInventory();
    }
}
