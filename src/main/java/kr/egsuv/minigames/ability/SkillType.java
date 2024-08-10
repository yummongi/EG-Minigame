package kr.egsuv.minigames.ability;

public enum SkillType {
    PRIMARY("주 스킬"),
    SECONDARY("보조 스킬"),
    PASSIVE("패시브"),
    ITEM_KIT("기본 키트");

    private final String skillName;

    SkillType(String skillName) {
        this.skillName = skillName;
    }

    @Override
    public String toString() {
        return skillName;
    }
}