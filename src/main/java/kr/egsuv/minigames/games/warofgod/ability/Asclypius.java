package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Asclypius extends Ability {

    private static final int COOLDOWN_PRIMARY = 20;
    private static final int COOLDOWN_SECONDARY = 40;

    public Asclypius(WarOfGodGame game, Player player) {
        super(game, player, "아스클리피어스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 3)) {
            startCooldown(SkillType.PRIMARY);
            healPlayer(player);  // 체력을 회복하는 메서드를 호출
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a치유되었습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 5)) {
            startCooldown(SkillType.SECONDARY);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a팀원을 치유하였습니다.", 10, 60, 20);

            game.getPlayers().stream()
                    .filter(p -> game.getPlayerTeam(p).equals(game.getPlayerTeam(player)) && !p.equals(player))
                    .forEach(this::healPlayer);  // 모든 팀원의 체력을 회복

        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 아스클리피어스는 패시브 스킬이 없습니다.
    }

    @Override
    public void itemSupply() {
        // 아스클리피어스는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 3개를 사용하여 자신의 체력을 전부 회복합니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 5개를 사용하여 팀원의 체력을 전부 회복합니다. (쿨타임: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "패시브 스킬이 없습니다.";
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
    }
}