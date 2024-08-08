package kr.egsuv.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemUtils {

    // 아이템 생성
    public static ItemStack createItem(Material material, int amount, String displayName, String... lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setMetaProperties(meta, displayName, lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // 아이템 생성
    public static ItemStack createItem(Material material, int amount, String displayName, Enchantment enchant, int enchantValue, String... lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setMetaProperties(meta, displayName, lore);
            meta.addEnchant(enchant, enchantValue, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // 공통 메타 설정 메서드
    private static void setMetaProperties(ItemMeta meta, String displayName, String... lore) {
        meta.displayName(Component.text(displayName).color(NamedTextColor.WHITE));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.text(line).color(NamedTextColor.GRAY));
        }
        meta.lore(loreComponents);
    }
}