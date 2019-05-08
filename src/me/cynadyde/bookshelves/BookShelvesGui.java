package me.cynadyde.bookshelves;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
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

    public static @Nullable BookShelvesGui getGUI(@NotNull String playerName) {
        for (BookShelvesGui gui : activeGUIs) {
            if (gui.player.getName().equalsIgnoreCase(playerName)) {
                return gui;
            }
        }
        return null;
    }

    public static @Nullable BookShelvesGui getGUI(@NotNull Inventory inventory) {
        for (BookShelvesGui gui : activeGUIs) {
            if (gui.view.getInventory().equals(inventory)) {
                return gui;
            }
        }
        return null;
    }

    public static void openGUI(@NotNull BookShelvesBlock bookshelf, @NotNull Player player) {
        activeGUIs.add(new BookShelvesGui(bookshelf, player));
    }

    public static final String titlePrefix = "[BookShelf] ";

//    public static final int slotsPerRow = 9;
//    public static final int rows = 3;
//    public static final int size = rows * slotsPerRow;
//
//    public static final int booksPerShelf = 5;
//    public static final int backButtonIndex = (slotsPerRow * 1) + 1;
//    public static final int nextButtonIndex = (slotsPerRow * 1) + 7;

    private Player player;
    private BookShelvesBlock bookShelf;
    private GuiViewer view;
//    private Inventory inventory;
//    private GuiStyle template;
//    private int totalPages;
//    private int currentPage;

    private BookShelvesGui(BookShelvesBlock bookShelf, Player player) {

        // TODO store guis as BookShelfBlock -> List<Player> in future? use xyz of block as hash
        // ACTUALLY, do you even need to store players? they can be retrieved from geView().getInventory()...

        this.player = player;
        this.bookShelf = bookShelf;

        GuiStyle template = GuiStyle.getStyle(bookShelf.getName(), bookShelf.getContents().size());
        this.view = (template != null) ? new TemplateViewer(template) : new DirectoryViewer();

        view.updateDisplay();
        player.openInventory(view.getInventory());
    }

    public Player getPlayer() {
        return player;
    }

    public BookShelvesBlock getBookShelf() {
        return bookShelf;
    }

    public GuiViewer getView() {
        return view;
    }

    public void close() {

        for (HumanEntity human : view.getInventory().getViewers()) {
            InventoryCloseEvent event = new InventoryCloseEvent(human.getOpenInventory());
            Bukkit.getPluginManager().callEvent(event);
        }
        activeGUIs.remove(this);
    }

    public interface GuiViewer {

        @NotNull Inventory getInventory();

        void updateDisplay();

        void slotClicked(@NotNull Player player, int slot);
    }

    public class TemplateViewer implements GuiViewer {

        private ItemStack prevButton = Utils.itemStack(1, Material.ITEM_FRAME, Utils.format("&4&lLast Shelf"));
        private ItemStack nextButton = Utils.itemStack(1, Material.ITEM_FRAME, Utils.format("&2&lNext Shelf"));

        private GuiStyle template;
        private Inventory inventory;
        private int currentPage;


        TemplateViewer(GuiStyle template) {

            this.template = template;

            ItemStack[] background = template.getBackground();
            this.inventory = Bukkit.createInventory(player, background.length);

            for (int i = 0; i < background.length; i++) {
                inventory.setItem(i, background[i]);
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

            // Make sure current page isn't out of bounds...
            currentPage = Math.max(0, Math.min(currentPage, totalPages));

            // Determine the start and end index of the current shelf...
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.max(startIndex, Math.min(startIndex + itemsPerPage, totalItems));

            // Get the shelf of books that will be displayed...
            List<ItemStack> viewedContents = (endIndex > startIndex) ?
                    bookShelf.getContents().subList(startIndex, endIndex) : new ArrayList<>();

            // Write in the forward-button if it should be displayed...
            inventory.setItem(template.prevBtnIndex(), (startIndex > 0) ?
                    prevButton : template.getBackground()[template.prevBtnIndex()]);

            // Write in the previous-button if it should be displayed...
            inventory.setItem(template.nextBtnIndex(), (endIndex < totalItems) ?
                    nextButton : template.getBackground()[template.nextBtnIndex()]);

            // Write in the content that should be displayed...
            for (int i = 0; i < itemsPerPage; i++) {

                int t = i + startIndex;
                inventory.setItem(template.contentIndices()[i], (t >= endIndex) ?
                        viewedContents.get(i) : template.getBackground()[i]);
            }
            player.updateInventory();
        }

        @Override
        public void slotClicked(@NotNull Player player, int slot) {


        }
    }

    public class DirectoryViewer implements GuiViewer {

        private Inventory inventory;
        private List<ItemStack> currentPath;

        DirectoryViewer() {

            this.inventory = null;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        @Override
        public void updateDisplay() {

            // draw last item in path to inventory
        }

        @Override
        public void slotClicked(@NotNull Player player, int slot) {

        }
    }
}
