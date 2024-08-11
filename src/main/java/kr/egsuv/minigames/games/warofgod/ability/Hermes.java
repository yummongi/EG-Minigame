package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Hermes extends Ability {

    private static final int COOLDOWN_PRIMARY = 82;

    public Hermes(WarOfGodGame game, Player player) {
        super(game, player, "헤르메스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 4)) {
            startCooldown(SkillType.PRIMARY);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);

            player.setAllowFlight(true);
            player.setFlying(true);

            new BukkitRunnable() {
                int remainingTime = 4;

                @Override
                public void run() {
                    if (!game.getPlayers().contains(player)) {
                        cancel();
                        return;
                    }

                    if (remainingTime > 0) {
                        player.sendTitle(ChatColor.RED + String.valueOf(remainingTime), "하늘의 가호가 사라지기까지", 10, 60, 20);
                        remainingTime--;
                    } else {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        player.sendTitle("", "하늘의 가호를 잃었습니다.", 10, 60, 20);
                        cancel();
                    }
                }
            }.runTaskTimer(game.getPlugin(), 0L, 20L);

        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        // 보조 스킬이 없는 경우
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬이 없는 경우
    }

    @Override
    public void itemSupply() {
        // 아이템 보급이 없는 경우
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 4개를 소모하여 4초간 비행합니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "패시브 스킬이 없습니다.";
    }
}