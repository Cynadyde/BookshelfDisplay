package me.cynadyde.bookshelves;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A functional bookshelf with a view of its content */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesBlock {

    // TODO control the gui display template using containers
    // TODO taking books from shelf unless no perms (hooking into other plugins) ???????
    // TODO "directory view" using nested containers

    public @Nullable static BookShelvesBlock from(@NotNull Block block) {

        // The given block's material is a bookshelf...
        if (!block.getType().equals(Material.BOOKSHELF)) {
            return null;
        }
        // Create a bookshelf object and updateDisplay its contents...
        BookShelvesBlock bookshelf = new BookShelvesBlock(block);
        bookshelf.updateContents();

        // If the bookshelf is empty, just return null...
        return (bookshelf.getContents().isEmpty())? null : bookshelf;
    }

    private Block anchor;
    private String name;
    private boolean hasGUI;
    private List<ItemStack> contents;

    private BookShelvesBlock(@NotNull Block block) {
        this.anchor = block;
        this.name = "Bookshelf";
        this.hasGUI = false;
        this.contents = new ArrayList<>();
    }

    public Block getAnchor() {
        return anchor;
    }

    public String getName() {
        return name;
    }

    public boolean hasGUI() {
        return hasGUI;
    }

    public List<ItemStack> getContents() {
        return contents;
    }

    public void updateContents() {

        this.hasGUI = false;
        this.contents.clear();

        // Look in all six directions from the bookshelf...
        World world = this.anchor.getWorld();
        for (BlockFace direction : new BlockFace[] {
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        }) {
            Block relBlock = this.anchor.getRelative(direction);
            for (Entity entity : (world.getNearbyEntities(relBlock.getBoundingBox()))) {
                if (entity.getType().equals(EntityType.ITEM_FRAME)) {

                    // For each item frame attached to the bookshelf...
                    ItemFrame itemFrame = (ItemFrame) entity;
                    if (itemFrame.getFacing().equals(direction)) {

                        // Add all books found in the item frame...
                        this.contents.addAll(processBooks(new ItemStack[] {itemFrame.getItem()}, true));
                    }
                }
            }
        }
        if (this.contents.size() > 1) {
            this.hasGUI = true;
        }
    }

    /* recursively gets a copy of each written book from a container and its nested containers */
    private @NotNull List<ItemStack> processBooks(@NotNull ItemStack[] items, boolean determineHasGUI) {

        List<ItemStack> results = new ArrayList<>();

        for (ItemStack item : items) {

            // If it contains a written book, add that book to the results...
            if (item.getType().equals(Material.WRITTEN_BOOK)) {
                results.add(item);
            }

            // If it contains a container item...
            ItemMeta itemMeta = item.getItemMeta();
            if (!(itemMeta instanceof BlockStateMeta)) {
                continue;
            }
            BlockState itemState = ((BlockStateMeta) itemMeta).getBlockState();
            if (!(itemState instanceof Container)) {
                continue;
            }

            // Recursively add all its written books to the results...
            Container container = (Container) itemState;
            results.addAll(processBooks(container.getInventory().getContents(), false));

            if (determineHasGUI) {
                this.hasGUI = true;
            }
        }
        return results;
    }
}
