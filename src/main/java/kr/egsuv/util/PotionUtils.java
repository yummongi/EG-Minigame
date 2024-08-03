package kr.egsuv.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionUtils {

    public static void applyPotionEffect(Player player, PotionEffectType potionType, int duration, int amplifier) {
        PotionEffect regeneration = new PotionEffect(potionType, duration, amplifier);
        player.addPotionEffect(regeneration);
    }
}