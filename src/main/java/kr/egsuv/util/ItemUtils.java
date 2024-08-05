package kr.egsuv.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ItemUtils {

    // 아이템 생성
    public static ItemStack createItem(Material material, int amount, String displayName, String... lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    // 아이템 생성
    public static ItemStack createItem(Material material, int amount, String displayName,  Enchantment enchant, int enchantValue, String... lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList(lore));
            meta.addEnchant(enchant, enchantValue, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
