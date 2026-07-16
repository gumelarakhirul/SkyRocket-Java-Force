package game.component;

public class PlayerScore implements Comparable<PlayerScore> {
    private String name;
    private int score;

    public PlayerScore(String name, int score) {
        this.name = name;

        this.score = score;
    }

    public String getName() { return name; }
    public int getScore() { return score; }

    @Override
    public int compareTo(PlayerScore o) {
        return Integer.compare(o.score, this.score); // Descending
    }
}
