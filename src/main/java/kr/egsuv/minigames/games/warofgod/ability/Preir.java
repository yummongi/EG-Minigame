package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.MinigameState;
import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Preir extends Ability {

    private static final int COOLDOWN_PRIMARY = 1;
    private boolean using = false;

    public Preir(WarOfGodGame game, Player player) {
        super(game, player, "프레이르");
        cooldownPrimary = COOLDOWN_PRIMARY;

        startPreirTimer();
    }

    private void startPreirTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getState() != MinigameState.IN_PROGRESS) {
                    this.cancel();
                } else if (using) {
                    for (Player teammate : game.getTeamMembers(game.getPlayerTeam(player))) {
                        if (teammate != null && teammate.isOnline() && game.getPlayers().contains(teammate)) {
                            teammate.setFoodLevel(20);
                        }
                    }
                }
            }
        }.runTaskTimer(game.getPlugin(), 0L, 100L);
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill() || player.isDead()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);

            for (Player teammate : game.getTeamMembers(game.getPlayerTeam(player))) {
                if (teammate != null && teammate.isOnline() && game.getPlayers().contains(teammate)) {
                    teammate.sendTitle("§c§l풍요", "§e§l아군 프레이르의 능력에 의해 이제 배고프지 않습니다.", 10, 60, 10);
                }
            }

            using = true;
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        // 보조 스킬 없음
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 없음
    }

    @Override
    public void itemSupply() {
        // 기본 아이템 제공 없음
    }

    @EventHandler
    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        using = false;
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 소모하여 팀원의 배고픔을 닳게 하지 않습니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
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