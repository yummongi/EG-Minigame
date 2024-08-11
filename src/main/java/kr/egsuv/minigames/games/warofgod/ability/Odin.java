package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class Odin extends Ability {

    private static final int COOLDOWN_PRIMARY = 18;
    private static final int COOLDOWN_SECONDARY = 90;

    public Odin(WarOfGodGame game, Player player) {
        super(game, player, "오딘");
        cooldownPrimary = COOLDOWN_PRIMARY;
        cooldownSecondary = COOLDOWN_SECONDARY;

        // 패시브나 아이템 공급이 없으므로 설정하지 않음
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);
            Fireball fireball = player.launchProjectile(Fireball.class);
            fireball.setIsIncendiary(true);
            fireball.setVelocity(player.getEyeLocation().getDirection().multiply(2f));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "불덩이를 날립니다", 10, 60, 20);
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 20)) {
            startCooldown(SkillType.SECONDARY);

            List<Player> enemies = game.getEnemyList(player);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_GATEWAY_SPAWN, 1.0f, 0.75f);
            player.sendTitle("", "마법을 발동합니다", 10, 60, 20);

            for (Player enemy : enemies) {
                if (enemy.getLocation().distance(player.getLocation()) <= 6) {
                    enemy.damage((int) player.getHealth() / 2, player);
                    enemy.setVelocity(new Vector(0, 1.2f, 0));
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 30, 0.15F, 0.15f, 0.15f, 0.07f);
                    enemy.sendTitle("", "오딘의 마법이 발동됐습니다.", 10, 60, 20);
                }
            }
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 소비하여 전방으로 불덩이를 날립니다. (재사용 대기시간: " + cooldownPrimary + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 20개를 소비하여 주변 6칸 내 적에게 자신의 체력의 절반만큼 데미지를 주고 공중으로 띄웁니다. (재사용 대기시간: " + cooldownSecondary + "초)";
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