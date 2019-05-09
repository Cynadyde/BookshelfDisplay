package me.cynadyde.bookshelves;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
class Utils {

    /**
     * Translates ampersands into color codes, then formats the string.
     */
    public static @NotNull String format(@NotNull String message, Object... objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    /**
     * Opens the book for the player without giving it to them.
     */
    public static void openBook(@NotNull Player player, @NotNull ItemStack book) {

        int slot = player.getInventory().getHeldItemSlot();
        ItemStack old = player.getInventory().getItem(slot);

        try {
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_BOOK);
            player.getInventory().setItem(slot, book);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
        catch (Exception ex) {

            Bukkit.getLogger().warning(String.format("Unable to open book for %s: %s", player.getName(), ex));
        }

        player.getInventory().setItem(slot, old);
    }

    /**
     * Creates an item stack with the given amount, material, and name.
     */
    public static @NotNull ItemStack itemStack(int amount, @NotNull Material material, @Nullable String name) {
        ItemStack itemStack = new ItemStack(material, amount);
        if (name != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    /**
     * Gets a container for the block if possible, else null.
     */
    public static @Nullable Container getContainer(Block block) {

        BlockState blockState = block.getState();
        if (!(blockState instanceof Container)) {
            return null;
        }
        return (Container) blockState;
    }

    /**
     * Gets a container for the item stack if possible, else null.
     */
    public static @Nullable Container getContainer(ItemStack item) {

        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return null;
        }
        BlockState itemState = ((BlockStateMeta) itemMeta).getBlockState();
        if (!(itemState instanceof Container)) {
            return null;
        }
        return (Container) itemState;
    }

    /**
     * Finds a container attached to the block either as an adjacent block or a container in an adjacent item-frame.
     * */
    public static @Nullable Container getAttachedContainer(@Nullable Block block) {

        if (block == null) {
            return null;
        }
        // Look in all six directions from the block...
        World world = block.getWorld();

        for (BlockFace direction : new BlockFace[] {
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        }) {
            // If the adjacent block is a container, use it...
            Block relBlock = block.getRelative(direction);
            Container blockContainer = Utils.getContainer(relBlock);

            if (blockContainer != null) {
                return blockContainer;
            }
            // Look through any entities in each adjacent block...
            for (Entity entity : (world.getNearbyEntities(relBlock.getLocation().add(0.5f, 0.5f, 0.5f), 0.5f, 0.5f, 0.5f))) {

                // Get the first item-frame attached to the block...
                if ((entity instanceof ItemFrame)) {
                    ItemFrame itemFrame = (ItemFrame) entity;

                    if (itemFrame.getAttachedFace().getOppositeFace().equals(direction)) {

                        // If the contained item is a container, return it...
                        ItemStack item = itemFrame.getItem();
                        Container itemContainer = Utils.getContainer(item);
                        if (itemContainer != null) {
                            return itemContainer;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Recursively gets each item at the list and at each nested container.
     * */
    public static List<ItemStack> collectItems(ItemStack[] items) {

        List<ItemStack> results = new ArrayList<>();

        for (ItemStack item : items) {

            // Add all items, including air and other containers...
            if (item == null) {
                results.add(new ItemStack(Material.AIR, 1));
                continue;
            }
            results.add(item);

            // If it is a container item...
            Container container = getContainer(item);
            if (container == null) {
                continue;
            }

            // Recursively add all its contents to the results...
            results.addAll(collectItems(container.getInventory().getContents()));
        }
        return results;
    }
}
