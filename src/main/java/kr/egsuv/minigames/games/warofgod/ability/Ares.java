package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class Ares extends Ability {

    private static final int DAMAGE_MULTIPLIER = 130; // 1.3배
    private static final int DAMAGE_AVOID_CHANCE = 10; // 10% 확률

    public Ares(WarOfGodGame game, Player player) {
        super(game, player, "아레스");
    }

    @Override
    public void primarySkill() {
        // 아레스는 주 스킬이 없습니다.
    }

    @Override
    public void secondarySkill() {
        // 아레스는 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 아레스의 패시브 스킬: 일정 확률로 공격을 회피
    }

    @Override
    public void itemSupply() {
        player.getInventory().addItem(new ItemStack(Material.STONE_SWORD, 1));
    }

    @Override
    public String getPrimaryDescription() {
        return "주 스킬이 없습니다.";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "공격 시 1.3배 피해를 주며, 10% 확률로 공격을 회피합니다.";
    }

    @EventHandler
    @Override
    public void onHitPlayer(EntityDamageByEntityEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        if (event.getDamager().equals(player)) {
            // 공격 시 피해량을 1.3배로 증가
            event.setDamage(event.getDamage() * (DAMAGE_MULTIPLIER / 100.0));
        }
    }

    @EventHandler
    @Override
    public void onEntityDamaged(EntityDamageEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        // 10% 확률로 피해 회피
        if (Math.random() * 100 < DAMAGE_AVOID_CHANCE) {
            event.setCancelled(true);
            player.sendTitle("", ChatColor.YELLOW + "공격 회피!", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }
}