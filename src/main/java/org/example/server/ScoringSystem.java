package org.example.server;


public class ScoringSystem {

    private static final int DEATH_PENALTY = 1;
    private static final int KILL_REWARD = 1;
    private static final int WIN_REWARD = 2;
    private static final int MIN_SCORE = 0;

    public static void penalizeKilledTank(Tank killed) {

        if (killed.score > MIN_SCORE) {
            killed.score -= DEATH_PENALTY;
        }
    }

    public static void rewardKillerTank(Tank killer) {
            killer.score += KILL_REWARD;
    }

    public static void rewardWinnerTank(Tank winner) {
            winner.score += WIN_REWARD;
    }


}