package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Hepaistos extends Ability {

    private static final int COOLDOWN_PRIMARY = 15;
    private static final int COOLDOWN_SECONDARY = 15;

    public Hepaistos(WarOfGodGame game, Player player) {
        super(game, player, "헤파이스토스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 22)) {
            startCooldown(SkillType.PRIMARY);
            player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 1));
            player.sendTitle("", "돌을 철로 변환하였습니다.", 10, 60, 20);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
        } else {
            player.sendTitle("", "돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill()) return;

        if (game.takeItem(player, Material.IRON_INGOT, 4)) {
            startCooldown(SkillType.SECONDARY);
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
            player.sendTitle("", "철을 다이아몬드로 변환하였습니다.", 10, 60, 20);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
        } else {
            player.sendTitle("", "철이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬은 onBlockBreak 이벤트로 처리합니다.
    }

    @Override
    public void itemSupply() {
        // 헤파이스토스는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 22개를 철 1개로 변환합니다. (재사용 대기시간: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "철 4개를 다이아몬드 1개로 변환합니다. (재사용 대기시간: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "돌을 캘 때 15% 확률로 돌 3개가 추가로 드랍됩니다.";
    }

    @EventHandler
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().equals(this.player)) return;
        if (event.getBlock().getType() == Material.COBBLESTONE) {
            if (Math.random() <= 0.15) { // 15% 확률로 돌 3개 추가 드랍
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 3));
            }
        }
    }
}