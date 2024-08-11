package kr.egsuv.minigames.ability;

public enum SkillType {
    PRIMARY("주스킬"),
    SECONDARY("보조스킬"),
    PASSIVE("패시브"),
    ITEM_KIT("기본 킷");

    private final String skillName;

    SkillType(String skillName) {
        this.skillName = skillName;
    }

    @Override
    public String toString() {
        return skillName;
    }
}