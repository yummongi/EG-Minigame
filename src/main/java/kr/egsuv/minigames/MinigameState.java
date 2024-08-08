package kr.egsuv.minigames;

public enum MinigameState {
    WAITING("준비중"),
    STARTING("시작중"),
    IN_PROGRESS("진행중"),
    ENDING("종료중"),
    DISABLED("비활성화"),
    REPAIRING("복구중");

    private final String displayName;

    MinigameState(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}