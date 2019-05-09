package me.cynadyde.bookshelves;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A functional bookshelf with a view of its content.
 */
@SuppressWarnings({ "WeakerAccess" })
public class BookShelvesContainer {

    /**
     * Gets the functional bookshelf at a given block if it exists.
     */
    public @Nullable static BookShelvesContainer at(@Nullable Block block) {

        Container container = Utils.getAttachedContainer(block);

        // bookshelves container must be a bookshelf block with a chest OR item-frame attached to it...
        if (block == null || !block.getType().equals(Material.BOOKSHELF) || container == null) {
            return null;
        }
        return new BookShelvesContainer(block, container);
    }

    private Block anchor;
    private Container root;

    public BookShelvesContainer(@NotNull Block block, @NotNull Container container) {

        this.anchor = block;
        this.root = container;
    }

    public @NotNull Block getAnchor() {
        return anchor;
    }

    public @NotNull Container getRoot() {
        return root;
    }
}
