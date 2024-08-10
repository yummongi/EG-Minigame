package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    @Override
    public void onKillPlayer(Player victim) {
        if (victim == null) return;

        player.setHealth(player.getMaxHealth()); // 체력을 전부 회복하는 메소드 호출
        player.sendTitle("", "적을 정복하여 체력을 회복합니다.", 10, 60, 20);
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(game.getPlugin(), () -> {
            if (game.getPlayers().contains(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }
        }, 60L);
    }

}