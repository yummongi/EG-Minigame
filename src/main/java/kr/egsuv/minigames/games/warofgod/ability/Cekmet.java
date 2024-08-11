package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Cekmet extends Ability {

    private static final int COOLDOWN_PRIMARY = 5;
    private static final int COOLDOWN_SECONDARY = 46;

    private Location savePos;

    public Cekmet(WarOfGodGame game, Player player) {
        super(game, player, "세크메트");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
        this.cooldownSecondary = COOLDOWN_SECONDARY;
    }

    @Override
    public void primarySkill() {
        if (!canUsePrimarySkill()) return;

        if (game.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);
            savePos = player.getTargetBlockExact(5).getLocation();
            if (savePos != null) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
                player.sendTitle("", "§a블록 위치 저장 완료", 10, 60, 20);
            } else {
                player.sendTitle("", "§c올바른 위치가 아닙니다.", 10, 60, 20);
            }
        } else {
            player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void secondarySkill() {
        if (!canUseSecondarySkill()) return;

        if (savePos != null) {
            if (game.takeItem(player, Material.COBBLESTONE, 5)) {
                startCooldown(SkillType.SECONDARY);
                player.getWorld().createExplosion(savePos.getX(), savePos.getY(), savePos.getZ(), 2.0f, true, false);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
                player.sendTitle("", "§a폭발을 일으켰습니다", 10, 60, 20);

                // 폭발 이후 주변 블록 파괴 로직
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        for (int k = -2; k <= 2; k++) {
                            Block block = savePos.getBlock().getRelative(i, j, k);
                            if (block.getType() != Material.DIAMOND_BLOCK && block.getType() != Material.CHEST
                                    && block.getType() != Material.TRAPPED_CHEST
                                    && block.getType() != Material.BARRIER
                                    && block.getType() != Material.BEDROCK) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
                savePos = null;
            } else {
                player.sendTitle("", "§c돌이 부족합니다.", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
            }
        } else {
            player.sendTitle("", "§c먼저 좌표를 지정해야 합니다", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.2f, 1.8f);
        }
    }

    @Override
    public void passiveSkill() {
        // 세크메트의 패시브 스킬은 구현되지 않았습니다.
    }

    @Override
    public void itemSupply() {
        // 세크메트는 초기 아이템 지급이 없습니다.
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 사용하여 5칸 내 바라보는 블럭의 위치 좌표를 저장합니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "코블스톤 5개를 소비하여 저장한 좌표에 폭발을 일으킵니다. 코어, 상자를 제외한 2칸 내 블럭이 파괴됩니다. (쿨타임: " + COOLDOWN_SECONDARY + "초)";
    }

    @Override
    public String getPassiveDescription() {
        return "세크메트는 패시브 스킬이 없습니다.";
    }
}