package me.cynadyde.bookshelves;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@SuppressWarnings("WeakerAccess")
public class Utils {

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
            ByteBuf buffer = Unpooled.buffer(256);
            buffer.setByte(0, (byte) 0);
            buffer.writerIndex(1);

            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CUSTOM_PAYLOAD);
            packet.getModifier().writeDefaults();
            packet.getModifier().write(1, MinecraftReflection.getPacketDataSerializer(buffer));
            packet.getStrings().write(0, "MC|BOpen");

            player.getInventory().setItem(slot, book);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
        catch (Exception ex) {

            Bukkit.getLogger().log(Level.WARNING, "Unable to open book for " + player.getName(), ex);
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
     * Recursively gets each item from the list and from each nested container.
     * */
    public static List<ItemStack> collectItems(ItemStack[] items, boolean containers) {

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

    /**
     * Creates an item stack from a string containing the material name and the display name,
     * separated by a space.
     */
    public static ItemStack buildItem(String string) {

        String[] params = string.split(" ", 1);  // TODO proper regex that removes quotes, too
        // TODO support for skull ids

        String matString = "";
        String nameString = null;

        if (params.length > 0) {
            matString = params[0];
        }
        if (params.length > 1) {
            nameString = params[1];
        }

        try {
            Material material = Material.valueOf(matString);
            return itemStack(1, material, nameString);
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
