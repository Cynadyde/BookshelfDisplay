package me.cynadyde.bookshelfdisplay;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * Utility functions for the BookshelfDisplay plugin.
 */
@SuppressWarnings({ "WeakerAccess" })
public class Utils {

    /**
     * Translate ampersands into color codes, then format the string.
     */
    public static @NotNull String format(@NotNull String message, Object... objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    /**
     * Open the book for a player without letting them have it.
     */
    public static void openBook(@NotNull Player player, @NotNull ItemStack book) {

        int handSlot = player.getInventory().getHeldItemSlot();
        ItemStack heldItem = player.getInventory().getItem(handSlot);

        try {

            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            PacketContainer packet;

            packet = protocolManager.createPacket(PacketType.Play.Server.CUSTOM_PAYLOAD);
            packet.getModifier().writeDefaults();

            packet.getStrings().write(0, "MC|BOpen");

            byte hand = (byte) EnumWrappers.Hand.MAIN_HAND.ordinal();
            ByteBuf buffer = Unpooled.buffer(256).setByte(0, hand).writerIndex(1);
            Object serializer = MinecraftReflection.getPacketDataSerializer(buffer);
            packet.getModifier().write(1, serializer);

            player.getInventory().setItem(handSlot, book);
            protocolManager.sendServerPacket(player, packet);
        }
        catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Unable to open book for " + player.getName(), ex);
        }
        finally {
            player.getInventory().setItem(handSlot, heldItem);
        }
    }

    /**
     * Create an item stack with the given amount, material, sub-id, and name.
     */
    public static @NotNull ItemStack itemStack(int amount, @NotNull Material material, int dmg, @Nullable String name) {
        ItemStack itemStack = new ItemStack(material, amount, (short) dmg);
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
     * Get a container for the block if possible, else null.
     */
    public static @Nullable Container getContainer(Block block) {

        BlockState blockState = block.getState();
        if (!(blockState instanceof Container)) {
            return null;
        }
        return (Container) blockState;
    }

    /**
     * Get a container for the item stack if possible, else null.
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
        // Set the container's custom name if possible...
        Container container = (Container) itemState;
        if (itemMeta.hasDisplayName()) {
            if (container instanceof Nameable) {
                ((Nameable) container).setCustomName(itemMeta.getDisplayName());
            }
        }
        return container;
    }

    /**
     * Determine if two containers have the same size and custom name.
     */
    public static boolean containerNamesEqual(@NotNull Container containerA, @NotNull Container containerB) {

        String a = (containerA instanceof Nameable) ? ((Nameable) containerA).getCustomName() : null;
        String b = (containerB instanceof Nameable) ? ((Nameable) containerB).getCustomName() : null;

        return (a == null && b == null) || (a != null && a.equals(b));
    }

    /**
     * Find a container attached to the block either as an adjacent block or a container in an adjacent item-frame.
     */
    public static @Nullable Container getAttachedContainer(@Nullable Block block) {

        if (block == null) {
            return null;
        }
        // Look in all six directions from the block...
        World world = block.getWorld();

        for (BlockFace direction : new BlockFace[]{
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

                            // Set the custom name of the container if possible...
                            ItemMeta itemMeta = item.getItemMeta();
                            if (itemMeta != null && itemMeta.hasDisplayName()) {
                                if (itemContainer instanceof Nameable) {
                                    ((Nameable) itemContainer).setCustomName(itemMeta.getDisplayName());
                                }
                            }
                            return itemContainer;
                        }
                    }
                }
            }
        }
        return null;
    }
}
