package kr.egsuv.minigames;

public enum TeamType {
    SOLO(1),
    DUO(2),
    TRIPLE(3),
    SQUAD(4);


    private final int playersPerTeam;

    TeamType(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }
}