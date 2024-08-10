package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class Tote extends Ability {

    private static final int COOLDOWN_PRIMARY = 13;
    private static final int COOLDOWN_SECONDARY = 95;

    private Location savedLocation = null;

    public Tote(WarOfGodGame game, Player player) {
        super(game, player, "토트");
        cooldownPrimary = COOLDOWN_PRIMARY;
        cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        startCooldown(SkillType.PRIMARY);
        savedLocation = player.getLocation().add(0, 1, 0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
        player.sendTitle("", "위치 좌표 저장완료", 10, 60, 20);
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill() || player.isDead()) return;

        if (savedLocation != null) {
            if (game.takeItem(player, Material.COBBLESTONE, 10)) {
                startCooldown(SkillType.SECONDARY);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 15, 0.5F, 0.5f, 0.5f, 0.1f);
                player.teleport(savedLocation, TeleportCause.PLUGIN);
                savedLocation = null;
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 15, 0.5F, 0.5f, 0.5f, 0.1f);
                player.sendTitle("", "좌표 이동 완료", 10, 60, 20);
            } else {
                player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
            }
        } else {
            player.sendTitle("", "먼저 좌표를 저장 해야합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public String getPrimaryDescription() {
        return "현재 위치 좌표를 저장해둡니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 10개를 사용하여 저장해둔 위치 좌표로 이동합니다. (재사용 대기시간: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "패시브 스킬이 없습니다.";
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 없음
    }

    @Override
    public void itemSupply() {
        // 기본 아이템 제공 없음
    }
}