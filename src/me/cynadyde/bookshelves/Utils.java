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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

@SuppressWarnings("WeakerAccess")
public class Utils {

    /* applies String.format and ChatColor.translateAlternateColorCodes */
    public static @NotNull String format(@NotNull String message, Object...objs) {
        return String.format(ChatColor.translateAlternateColorCodes('&', message), objs);
    }

    /* have the player read a book that they do not possess */
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

    /* Creates an item stack with the given amount, material, and name */
    public static @NotNull ItemStack itemStack(int amount, @NotNull Material material,@Nullable String name) {
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
}
