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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A functional bookshelf with a view of its content.
 * */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class BookShelvesBlock {

    public @Nullable static BookShelvesBlock from(@NotNull Block block) {

        // The given block's material is a bookshelf...
        if (!block.getType().equals(Material.BOOKSHELF)) {
            return null;
        }
        // Create a bookshelf object and update its contents...
        BookShelvesBlock bookshelf = new BookShelvesBlock(block);
        bookshelf.updateRoot();

        // If the bookshelf has no root, just return null...
        return (bookshelf.getRoot() == null) ? null : bookshelf;
    }

    private Block anchor;
    private String name;
    private ItemStack root;
    private List<ItemStack> contents;

    private BookShelvesBlock(@NotNull Block block) {
        this.anchor = block;
        this.name = null;
        this.root = null;
        this.contents = new ArrayList<>();
    }

    public @NotNull Block getAnchor() {
        return anchor;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable ItemStack getRoot() {
        return root;
    }

    public @NotNull List<ItemStack> getContents() {
        return contents;
    }

    public boolean isRootContainer() {
        return (Utils.getContainer(this.root) != null);
    }

    /**
     * Sets the item in the item frame attached to the bookshelf as the root.
     * If the root item was a container with a custom name, also sets the bookshelf's name.
     * */
    public void updateRoot() {

        this.name = null;
        this.root = null;

        // Look in all six directions from the bookshelf...
        World world = this.anchor.getWorld();
        for (BlockFace direction : new BlockFace[] {
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        }) {
            Block relBlock = this.anchor.getRelative(direction);
            for (Entity entity : (world.getNearbyEntities(relBlock.getBoundingBox()))) {
                if (entity.getType().equals(EntityType.ITEM_FRAME)) {

                    // Get the first item frame attached to the bookshelf...
                    ItemFrame itemFrame = (ItemFrame) entity;
                    if (itemFrame.getFacing().equals(direction)) {

                        // Save its item as the root...
                        this.root = itemFrame.getItem();

                        // Set the name of the bookshelf if the item was a container with a custom name...
                        Container container = Utils.getContainer(this.root);
                        if (container != null) {
                            this.name = container.getCustomName();
                        }
                        return;
                    }
                }
            }
        }
    }

    /**
     * Updates the bookshelf's list of all items contained in the root.
     * */
    public void updateContents() {

        this.contents.clear();

        // Add all items found in the item frame to the bookshelf content...
        this.contents.addAll(processContents(new ItemStack[] {this.root}, true));
    }

    /**
     * Recursively gets each item from the list and from each nested container.
     * */
    private List<ItemStack> processContents(ItemStack[] items, boolean determineHasGUI) {

        List<ItemStack> results = new ArrayList<>();

        for (ItemStack item : items) {

            // Add all items, including air...
            if (item == null) {
                results.add(new ItemStack(Material.AIR, 1));
                continue;
            }
            results.add(item);

            // If it is a container item...
            Container container = Utils.getContainer(item);
            if (container == null) {
                continue;
            }

            // Recursively add all its contents to the results...
            results.addAll(processContents(container.getInventory().getContents(), false));
        }
        return results;
    }
}
