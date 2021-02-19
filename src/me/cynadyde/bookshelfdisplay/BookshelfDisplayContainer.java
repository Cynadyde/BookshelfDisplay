package me.cynadyde.bookshelfdisplay;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A functional bookshelf with a view of its content.
 */
public class BookshelfDisplayContainer {

    private Block anchor;
    private Container root;

    /**
     * Create a representation of a bookshelf's container.
     */
    public BookshelfDisplayContainer(@NotNull Block block, @NotNull Container container) {
        this.anchor = block;
        this.root = container;
    }

    /**
     * Get the functional bookshelf at a given block if it exists.
     */
    public @Nullable static BookshelfDisplayContainer at(@Nullable Block block) {

        Container container = Utils.getAttachedContainer(block);

        // bookshelfdisplay container must be a bookshelf block with a chest OR item-frame attached to it...
        if (block == null || block.getType() != Material.BOOKSHELF || container == null) {
            return null;
        }
        return new BookshelfDisplayContainer(block, container);
    }

    /**
     * Get the bookshelf's actual block.
     */
    public @NotNull Block getAnchor() {
        return anchor;
    }

    /**
     * Get the root of the bookshelf's contents.
     */
    public @NotNull Container getRoot() {
        return root;
    }
}
