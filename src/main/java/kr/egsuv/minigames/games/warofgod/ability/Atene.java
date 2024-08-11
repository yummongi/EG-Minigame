package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class Atene extends Ability {

    private static final int COOLDOWN_PRIMARY = 2;
    private static final int COOLDOWN_SECONDARY = 150;

    public Atene(WarOfGodGame game, Player player) {
        super(game, player, "아테나");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 4)) {
            startCooldown(SkillType.PRIMARY);
            player.getInventory().addItem(new ItemStack(Material.BOOK, 1));
            player.getInventory().addItem(new ItemStack(Material.LAPIS_LAZULI, 2));  // 청금석 아이템 추가
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a돌을 책과 청금석으로 변환하였습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 64)) {
            startCooldown(SkillType.SECONDARY);
            player.getInventory().addItem(new ItemStack(Material.ENCHANTING_TABLE, 1));  // 인첸트 테이블 아이템 추가
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a돌을 지혜의 상자로 변환하였습니다.", 10, 60, 20);
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 아테나의 패시브 스킬: 팀원이 사망할 때마다 레벨 1 증가
        // 해당 기능은 onPlayerDeath 이벤트에서 처리합니다.
    }

    @Override
    public void itemSupply() {
        // 아테나는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 4개로 책과 청금석을 얻습니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 64개를 사용하여 인첸트 테이블을 얻습니다. (쿨타임: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "자신을 제외한 팀원이 사망할 때마다 레벨 1을 얻습니다.";
    }

    @EventHandler
    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        if (!game.getPlayers().contains(deceased) || deceased.equals(this.player)) return;
        if (game.getPlayerTeam(deceased).equals(game.getPlayerTeam(this.player))) {
            this.player.giveExpLevels(1);
            this.player.sendMessage("§a팀원이 사망하여 경험치가 증가했습니다.");
        }
    }

}