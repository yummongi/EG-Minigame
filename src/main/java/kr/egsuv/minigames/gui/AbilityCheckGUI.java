package kr.egsuv.minigames.gui;

import kr.egsuv.minigames.ability.Ability;
import kr.egsuv.minigames.games.warofgod.WarOfGodGame;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbilityCheckGUI {
    private final WarOfGodGame game;

    public AbilityCheckGUI(WarOfGodGame game) {
        this.game = game;
    }

    public void giveAbilityCheckBook(Player player) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.displayName(Component.text("§7[ §c능력 확인 §7]"));
        meta.lore(Arrays.asList(Component.text("§7§l| §f우클릭 시 부여받은 신의 능력을 확인합니다.")));
        book.setItemMeta(meta);
        player.getInventory().setItem(8, book);
    }

    public void openAbilityGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("§0자신의 능력: §c" + game.getPlayerAbilities().get(player).getAbilityName()));

        ItemStack allGodsList = createItem(Material.BOOK, "§f모든 신 목록", Arrays.asList(
                "§7클릭하여 모든 신의 목록을 확인하세요."
        ));
        gui.setItem(11, allGodsList);

        Ability playerAbility = game.getPlayerAbilities().get(player);

        // 설명 줄바꿈 처리
        List<String> abilityLore = new ArrayList<>();
        abilityLore.addAll(wrapText("§7§l| §e주스킬: §f" + playerAbility.getPrimaryDescription(), 30));
        abilityLore.addAll(wrapText("§7§l| §e보조스킬: §f" + playerAbility.getSecondaryDescription(), 30));
        abilityLore.addAll(wrapText("§7§l| §e패시브: §f" + playerAbility.getPassiveDescription(), 30));

        ItemStack abilityInfo = createItem(Material.BLAZE_ROD, "§f" + playerAbility.getAbilityName() + " 능력 정보", abilityLore);
        gui.setItem(15, abilityInfo);

        player.openInventory(gui);
    }
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).collect(java.util.stream.Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    public void openAllGodsList(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("§0신 목록")); // 4줄 (36칸) 크기의 GUI 생성

        int slot = 0;
        for (Class<? extends Ability> abilityClass : game.getAbilityClasses()) {
            try {
                Ability ability = abilityClass.getConstructor(WarOfGodGame.class, Player.class).newInstance(game, player);
                ItemStack icon = new ItemStack(Material.NETHER_STAR);
                ItemMeta meta = icon.getItemMeta();
                meta.displayName(Component.text("§c§l" + ability.getAbilityName()));

                // 설명 줄바꿈 처리
                List<String> lore = new ArrayList<>();
                lore.addAll(wrapText("§7§l| §e주스킬: §f" + ability.getPrimaryDescription(), 30));
                lore.addAll(wrapText("§7§l| §e보조스킬: §f" + ability.getSecondaryDescription(), 30));
                lore.addAll(wrapText("§7§l| §e패시브: §f" + ability.getPassiveDescription(), 30));

                meta.setLore(lore);
                icon.setItemMeta(meta);
                gui.setItem(slot++, icon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 뒤로가기 버튼 추가
        ItemStack backButton = createItem(Material.BARRIER, "§c뒤로가기", Arrays.asList(
                "§7이전 메뉴로 돌아갑니다."
        ));
        gui.setItem(35, backButton); // 제일 오른쪽 아래에 위치시킴

        player.openInventory(gui);
    }
    private List<String> wrapText(String text, int lineLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder("§f"); // 처음 라인에 들여쓰기와 색상 코드 추가

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > lineLength) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder("§f   " + word); // 새로운 라인에 들여쓰기와 색상 코드 추가
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
    // 인벤토리 클릭 이벤트 처리
    public void handleInventoryClick(Player player, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());

        if (displayName.equals("§c뒤로가기")) {
            openAbilityGUI(player); // 뒤로가기 버튼을 클릭하면 이전 메뉴로 돌아감
            event.setCancelled(true); // 이벤트를 취소하여 아이템을 움직이지 않도록 함
        }
    }
}