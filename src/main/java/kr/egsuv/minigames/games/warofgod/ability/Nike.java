package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Nike extends Ability {

    public Nike(WarOfGodGame game, Player player) {
        super(game, player, "니케");

        // 패시브 효과: 기본 이동 속도 증가
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
    }

    @Override
    public void primarySkill() {
        // 니케는 주 스킬이 없습니다.
    }

    @Override
    public void secondarySkill() {
        // 니케는 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬: 적을 처치할 시 체력 전부 회복
    }

    @Override
    public void itemSupply() {
        // 니케는 기본 아이템 제공이 없습니다.
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
        return "적을 처치할 시 체력을 전부 회복합니다. 기본 이동 속도 버프를 받습니다.";
    }

    @EventHandler
    public void onKillPlayer(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.equals(this.player)) return;

        killer.setHealth(killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        killer.sendTitle("", "적을 정복하여 체력을 회복합니다.", 10, 60, 20);
    }

    @EventHandler
    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.getPlayer().equals(this.player)) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(game.getPlugin(), () -> {
            if (game.getPlayers().contains(this.player)) {
                this.player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }
        }, 60L);
    }

}