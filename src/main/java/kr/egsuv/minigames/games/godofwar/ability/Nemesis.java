package kr.egsuv.minigames.games.godofwar.ability;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.godofwar.WarOfGodGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Nemesis extends Ability {

    private String lastKiller = "";
    private final PotionEffect slow = new PotionEffect(PotionEffectType.SLOWNESS, 60, 4);
    private final PotionEffect buff1 = new PotionEffect(PotionEffectType.SPEED, 200, 1);
    private final PotionEffect buff2 = new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 1);
    private final PotionEffect buff3 = new PotionEffect(PotionEffectType.REGENERATION, 200, 1);

    public Nemesis(WarOfGodGame game, Player player) {
        super(game, player, "네메시스");
    }

    @Override
    public String getPrimaryDescription() {
        return "기본 스킬이 없습니다.";
    }

    @Override
    public String getSecondaryDescription() {
        return "보조 스킬이 없습니다.";
    }

    @Override
    public String getPassiveDescription() {
        return "자신을 죽인 적은 3초간 구속 5 버프를 받습니다.\n"
                + "또한 자신을 마지막으로 죽인 적을 자신이 처치할 시 10초간 신속2, 점프강화2, 재생2 버프를 받습니다.\n"
                + "복수에 성공하면 마지막으로 죽인 적이 초기화됩니다.";
    }

    @Override
    public void primarySkill() {
        // 네메시스는 기본 스킬이 없습니다.
    }

    @Override
    public void secondarySkill() {
        // 네메시스는 보조 스킬이 없습니다.
    }

    @Override
    public void passiveSkill() {
        // 패시브 스킬 로직은 onPlayerDeath 메소드에 포함되어 있습니다.
    }

    @Override
    public void itemSupply() {
        // 네메시스는 아이템 보급이 없습니다.
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Player k = p.getKiller();
        if (k != null) {
            lastKiller = k.getName();
            k.addPotionEffect(slow);
        }
    }

    @Override
    public void onKillPlayer(Player victim) {
        if (victim == null) return;
        Player p = Bukkit.getPlayer(player.getName());
        if (p != null && victim.getName().equalsIgnoreCase(lastKiller)) {
            p.sendTitle("§c§l복수 성공", "§a§l버프를 받습니다.", 0, 100, 0);
            p.addPotionEffect(buff1);
            p.addPotionEffect(buff2);
            p.addPotionEffect(buff3);
            lastKiller = "";
        }
    }
}