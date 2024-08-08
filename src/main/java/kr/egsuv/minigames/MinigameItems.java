package kr.egsuv.minigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MinigameItems {

    private Random random;

    public MinigameItems() {
        random = new Random();
    }

    private void giveRandomItem(Player player) {
        ItemStack item = null;
        switch (random.nextInt(3)) {
            case 0:
                item = createTeleportBook();
                break;
            case 1:
                item = new ItemStack(Material.ENDER_PEARL, 3); // 엔더 진주 3개
                break;
            case 2:
                item = new ItemStack(Material.FIREWORK_ROCKET, 5); // 불꽃 로켓 5개
                break;
        }
        if (item != null) {
            player.getInventory().addItem(item);
        }
    }

    private ItemStack createTeleportBook() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.displayName(Component.text("텔레포트 책").color(NamedTextColor.LIGHT_PURPLE));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("우클릭 시 랜덤 스폰 위치로 이동합니다.").color(NamedTextColor.GRAY));
        meta.lore(lore);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createSpeedPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.displayName(Component.text("스피드 포션").color(NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("사용 시 스피드 효과를 부여합니다.").color(NamedTextColor.GRAY));
        meta.lore(lore);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1), true);
        potion.setItemMeta(meta);
        return potion;
    }

    private List<ItemStack> createSpecialItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createTeleportBook());
        items.add(createSpeedPotion());
        // 더 많은 아이템 추가 가능
        return items;
    }
}