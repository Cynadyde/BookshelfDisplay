package me.cynadyde.bookshelves;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings({ "WeakerAccess", "SameParameterValue", "unused", "PointlessArithmeticExpression" })
public class BookShelvesGui {

    private static final Set<BookShelvesGui> activeGUIs = new HashSet<>();

    public static @NotNull Set<BookShelvesGui> activeGUIs() {
        return Collections.unmodifiableSet(activeGUIs);
    }

    /**
     * Fetches an opened gui from a known inventory.
     */
    public static @Nullable BookShelvesGui getGUI(@Nullable Inventory inventory) {

        if (inventory != null) {
            for (BookShelvesGui gui : activeGUIs) {
                if (gui.getView().getInventory().equals(inventory)) {
                    return gui;
                }
            }
        }
        return null;
    }

    /**
     * Fetches an opened gui from a known block.
     */
    public static @Nullable BookShelvesGui getGui(@Nullable Block block) {

        if (block != null) {
            for (BookShelvesGui gui : activeGUIs) {
                if (gui.getBookShelf().getAnchor().equals(block)) {
                    return gui;
                }
            }
        }
        return null;
    }

    /**
     * Fetches an opened gui from a known owner.
     */
    public static @Nullable BookShelvesGui getGUI(@Nullable Player owner) {

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
    public static void openGUI(@NotNull BookShelvesBlock bookshelf, @NotNull Player player) {
        BookShelvesGui gui = new BookShelvesGui(bookshelf, player);
        player.openInventory(gui.getView().getInventory());
        activeGUIs.add(gui);
    }

    public static final String titlePrefix = "[BookShelf] ";

    private BookShelvesBlock bookShelf;
    private Player owner;
    private BookShelvesView view;

    private BookShelvesGui(@NotNull BookShelvesBlock bookShelf, @NotNull Player owner) {

        this.owner = owner;
        this.bookShelf = bookShelf;

        GuiStyle template = GuiStyle.getStyle(null, bookShelf.getContents().size());
        // this.view = (template != null) ? new TemplateView(template) : new DirectoryView();
        assert (template != null);
        this.view = new TemplateView(template);

        view.updateDisplay();
    }

    public @NotNull BookShelvesBlock getBookShelf() {
        return bookShelf;
    }

    public @NotNull Player getOwner() {
        return owner;
    }

    public @NotNull BookShelvesView getView() {
        return view;
    }

    public void close() {

        for (HumanEntity human : view.getInventory().getViewers()) {
            InventoryCloseEvent event = new InventoryCloseEvent(human.getOpenInventory());
            Bukkit.getPluginManager().callEvent(event);
        }
        activeGUIs.remove(this);
    }

    /**
     * Links the contents of a bookshelf to an inventory.
     */
    public interface BookShelvesView {

        @NotNull Inventory getInventory();

        void updateDisplay();

        void slotClicked(@NotNull Player player, int slot);
    }

    /**
     * Uses a style template to display the contents of a bookshelf in an inventory.
     */
    public class TemplateView implements BookShelvesView {

        private ItemStack prevButton = Utils.itemStack(1, Material.ITEM_FRAME, Utils.format("&4&lLast Shelf"));
        private ItemStack nextButton = Utils.itemStack(1, Material.ITEM_FRAME, Utils.format("&2&lNext Shelf"));

        private GuiStyle template;
        private Inventory inventory;
        private int currentPage;


        TemplateView(GuiStyle template) {

            this.template = template;

            ItemStack[] trim = GuiStyle.getNavBarBackground();
            ItemStack[] background = template.getBackground();

            this.inventory = Bukkit.createInventory(owner, trim.length + background.length + trim.length);

            int i = 0;
            for (ItemStack item : trim) {
                inventory.setItem(i, item);
                i++;
            }
            for (ItemStack item : background) {
                inventory.setItem(i, item);
                i++;
            }
            for (ItemStack item : trim) {
                inventory.setItem(i, item);
                i++;
            }
            this.currentPage = 0;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        @Override
        public void updateDisplay() {

            int totalItems = bookShelf.getContents().size();
            int itemsPerPage = template.contentIndices().length;
            int totalPages = (int) Math.ceil(totalItems / (double) itemsPerPage);

            currentPage = Math.max(0, Math.min(currentPage, totalPages));

            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.max(startIndex, Math.min(startIndex + itemsPerPage, totalItems));

            // Get the page of books that will be displayed...
            List<ItemStack> viewedContents = (endIndex > startIndex) ?
                    bookShelf.getContents().subList(startIndex, endIndex) : new ArrayList<>();

            // Write in the previous-button if it should be displayed...
            inventory.setItem(template.prevBtnIndex(), (startIndex > 0) ?
                    prevButton : template.getBackground()[template.prevBtnIndex()]);

            // Write in the next-button if it should be displayed...
            inventory.setItem(template.nextBtnIndex(), (endIndex < totalItems) ?
                    nextButton : template.getBackground()[template.nextBtnIndex()]);

            // Write in the content that should be displayed...
            for (int i = 0; i < itemsPerPage; i++) {

                int t = i + startIndex;
                inventory.setItem(template.contentIndices()[i], (t >= endIndex) ?
                        viewedContents.get(i) : template.getBackground()[i]);
            }
            // Update all player-viewers' inventories...
            for (HumanEntity human : inventory.getViewers()) {
                if (human instanceof Player) {
                    ((Player) human).updateInventory();
                }
            }
        }

        @Override
        public void slotClicked(@NotNull Player player, int slot) {


        }
    }

//    public class DirectoryView implements BookShelvesView {
//
//        private Inventory inventory;
//        private List<ItemStack> currentPath;
//
//        DirectoryView() {
//
//            this.inventory = null;
//        }
//
//        @Override
//        public @NotNull Inventory getInventory() {
//            return inventory;
//        }
//
//        @Override
//        public void updateDisplay() {
//
//            // draw last item in path to inventory
//        }
//
//        @Override
//        public void slotClicked(@NotNull Player player, int slot) {
//
//        }
//    }
}
