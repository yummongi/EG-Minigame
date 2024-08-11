package kr.egsuv.minigames.games.warofgod.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.ability.SkillType;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Agnes extends Ability {
    private static final int COOLDOWN_PRIMARY = 3;

    public Agnes(WarOfGodGame game, Player player) {
        super(game, player, "아그네스");
        this.cooldownPrimary = COOLDOWN_PRIMARY;
    }

    @Override
    public void primarySkill() {
        WarOfGodGame warOfGodGame = (WarOfGodGame) game;
        if (warOfGodGame.takeItem(player, Material.COBBLESTONE, 1)) {
            startCooldown(SkillType.PRIMARY);
            int randomItem = (int) (Math.random() * 3);
            ItemStack giftItem;
            switch (randomItem) {
                case 0:
                    giftItem = new ItemStack(Material.FLINT, 1);
                    break;
                case 1:
                    giftItem = new ItemStack(Material.FEATHER, 1);
                    break;
                default:
                    giftItem = new ItemStack(Material.STRING, 1);
            }
            player.getInventory().addItem(giftItem);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 0.5f);
            player.sendTitle("", "§a변환 완료", 10, 60, 20);
        } else {
            player.sendTitle("", "§c재료가 부족합니다", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    @Override
    public void secondarySkill() {
        // 아그네스는 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 아그네스의 패시브 스킬: 중력 없는 화살
    }

    @Override
    public void itemSupply() {
        player.getInventory().addItem(new ItemStack(Material.BOW, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 12));
    }

    @Override
    public String getPrimaryDescription() {
        return "코블스톤 1개를 소모하여 부싯돌, 깃털, 실 중 하나로 변환합니다. (쿨타임: " + COOLDOWN_PRIMARY + "초)";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "발사한 화살에 중력이 적용되지 않습니다.";
    }

    @EventHandler
    @Override
    public void onPlayerShootBow(EntityShootBowEvent event) {
        if (!event.getEntity().equals(this.player)) return;
        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isDead() || arrow.isInBlock()) {
                        this.cancel();
                        return;
                    }
                    arrow.setVelocity(arrow.getVelocity().normalize().multiply(1.5));
                }
            }.runTaskTimer(game.getPlugin(), 1L, 1L);
        }
    }
}