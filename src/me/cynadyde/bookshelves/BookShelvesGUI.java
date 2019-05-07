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
// TODO refactor GUI to Gui
public class BookShelvesGUI {

    // TODO build gui from template

    // Define the items used in the GUI
    private static ItemStack backButton = Utils.itemStack(1, Material.ITEM_FRAME, Utils.format("&4&lLast Shelf"));
    private static ItemStack forwardButton = Utils.itemStack(1, Material.ITEM_FRAME, Utils.format("&2&lNext Shelf"));
//    private static ItemStack blackBG = Utils.itemStack(1, Material.BLACK_STAINED_GLASS_PANE, " ");
//    private static ItemStack blueBG = Utils.itemStack(1, Material.BLUE_STAINED_GLASS_PANE, " ");
//    private static ItemStack redBG = Utils.itemStack(1, Material.RED_STAINED_GLASS_PANE, " ");
//    private static ItemStack purpleBG = Utils.itemStack(1, Material.PURPLE_STAINED_GLASS_PANE, " ");


    // The make-up of the background of the GUI
//    private static final ItemStack[] background = new ItemStack[]{
//            blackBG, blueBG, redBG, redBG, redBG, redBG, redBG, blueBG, blackBG,
//            blackBG, blueBG, purpleBG, purpleBG, purpleBG, purpleBG, purpleBG, blueBG, blackBG,
//            blackBG, blueBG, blackBG, blackBG, blackBG, blackBG, blackBG, blueBG, blackBG
//    };

    private static final Set<BookShelvesGUI> activeGUIs = new HashSet<>();

    public static @NotNull Set<BookShelvesGUI> activeGUIs() {
        return Collections.unmodifiableSet(activeGUIs);
    }

    public static @Nullable BookShelvesGUI getGUI(@NotNull String playerName) {
        for (BookShelvesGUI gui : activeGUIs) {
            if (gui.player.getName().equalsIgnoreCase(playerName)) {
                return gui;
            }
        }
        return null;
    }

    public static @Nullable BookShelvesGUI getGUI(@NotNull Inventory inventory) {
        for (BookShelvesGUI gui : activeGUIs) {
            if (gui.view.getInventory().equals(inventory)) {
                return gui;
            }
        }
        return null;
    }

    public static void openGUI(@NotNull Player player, @NotNull BookShelvesBlock bookshelf) {
        activeGUIs.add(new BookShelvesGUI(player, bookshelf));
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
//    private GuiTemplate template;
//    private int maxPage;
//    private int currentPage;

    private BookShelvesGUI(Player player, BookShelvesBlock bookShelf) {

        // TODO store guis as BookShelfBlock -> List<Player> in future? use xyz of block as hash
        // ACTUALLY, do you even need to store players? they can be retrieved from geView().getInventory()...

        this.player = player;
        this.bookShelf = bookShelf;

        GuiTemplate template = GuiTemplate.getTemplate(bookShelf.getName(), bookShelf.getContents().size());
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

        for (HumanEntity human : inventory.getViewers()) {
            InventoryCloseEvent event = new InventoryCloseEvent(human.getOpenInventory());
            Bukkit.getPluginManager().callEvent(event);
        }
        activeGUIs.remove(this);
    }

    public void slotClicked(Player player, int slot) {

    }

    private interface GuiViewer {

        @NotNull Inventory getInventory();

        void updateDisplay();

        void slotClicked(@NotNull Player player, int slot);
    }

    private class TemplateViewer implements GuiViewer {

        private GuiTemplate template;
        private Inventory inventory;
        private int maxPage;
        private int currentPage;

        TemplateViewer(GuiTemplate template) {

            this.template = template;

            ItemStack[] background = template.getBackground();
            this.inventory = Bukkit.createInventory(player, background.length);

            for (int i = 0; i < background.length; i++) {
                inventory.setItem(i, background[i]);
            }
            this.maxPage = (int) Math.ceil(bookShelf.getContents().size() / (double) template.getContent().length);
            this.currentPage = 0;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        @Override
        public void updateDisplay() {

            currentPage = Math.max(0, Math.min(currentPage + pageChange, maxPage));

            // Get the shelf of books that will be displayed...
            int viewingIndex = currentPage * booksPerShelf;
            int startIndex = Math.max(0, Math.min(viewingIndex, bookShelf.getContents().size() - 1));
            int endIndex = Math.max(startIndex, Math.min(startIndex + booksPerShelf, bookShelf.getContents().size()));

            List<ItemStack> viewedContents = (endIndex > startIndex) ? bookShelf.getContents().subList(startIndex, endIndex) : new ArrayList<>();

            // Write in the navigation buttons if they should be displayed...
            inventory.setItem(backButtonIndex, (startIndex > 0) ? backButton : background[backButtonIndex]);
            inventory.setItem(nextButtonIndex, (endIndex < bookShelf.getContents().size()) ? forwardButton : background[nextButtonIndex]);

            // Write in the shelf's books or their background counterpart...
            for (int i = 0; i < 5; i++) {
                inventory.setItem((slotsPerRow * 1) + 2 + i, (i > viewedContents.size() - 1) ?
                        background[(slotsPerRow * 1) + 2 + i] : viewedContents.get(i));
            }
            player.updateInventory();
        }

        @Override
        public void slotClicked(@NotNull Player player, int slot) {


        }
    }

    private class DirectoryViewer implements GuiViewer {

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
