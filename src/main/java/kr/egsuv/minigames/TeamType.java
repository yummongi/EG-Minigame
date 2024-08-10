package kr.egsuv.minigames;

public enum TeamType {
    SOLO(1),
    DUO(1),
    TRIPLE(3),
    SQUAD(1);


    private final int playersPerTeam;

    TeamType(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public int setPlayersPerTeam() {
        return playersPerTeam;
    }
}