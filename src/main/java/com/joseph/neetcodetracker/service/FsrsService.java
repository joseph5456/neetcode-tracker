package com.joseph.neetcodetracker.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Implements the FSRS-4.5 (Free Spaced Repetition Scheduler) algorithm.
 * Pure functions only — no persistence, no Spring context dependencies beyond
 * the @Service annotation for DI — so this class can be unit tested in isolation
 * without spinning up the application context.
 *
 * Reference: https://github.com/open-spaced-repetition/fsrs4anki
 */
@Service
public class FsrsService {

    /** Published FSRS-4.5 default parameter set (17 weights). */
    public static final double[] W = {
            0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49, 0.14,
            0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
    };

    /** Target probability of recall used to derive the next interval. */
    public static final double TARGET_RETENTION = 0.9;

    public enum Rating {
        AGAIN(1), HARD(2), GOOD(3), EASY(4);
        public final int value;
        Rating(int value) { this.value = value; }
    }

    /** Result of scheduling a single review. */
    public record ScheduleResult(
            double difficulty,
            double stability,
            int reps,
            int lapses,
            Instant lastReview,
            Instant due
    ) {}

    /** Current state of a card going into a review (reps == 0 means never reviewed). */
    public record CardState(
            double difficulty,
            double stability,
            int reps,
            int lapses,
            Instant lastReview
    ) {
        public static CardState fresh() {
            return new CardState(0, 0, 0, 0, null);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    double initStability(Rating rating) {
        return W[rating.value - 1];
    }

    double initDifficulty(Rating rating) {
        return clamp(W[4] - (rating.value - 3) * W[5], 1, 10);
    }

    double nextDifficulty(double difficulty, Rating rating) {
        double newD = difficulty - W[6] * (rating.value - 3);
        double reverted = W[7] * initDifficulty(Rating.EASY) + (1 - W[7]) * newD;
        return clamp(reverted, 1, 10);
    }

    /** Probability of recall given elapsed days since last review and current stability. */
    double retrievability(double elapsedDays, double stability) {
        return Math.pow(1 + elapsedDays / (9 * stability), -1);
    }

    double nextStability(double difficulty, double stability, double retrievability, Rating rating) {
        if (rating == Rating.AGAIN) {
            return W[11] * Math.pow(difficulty, -W[12])
                    * (Math.pow(stability + 1, W[13]) - 1)
                    * Math.exp(W[14] * (1 - retrievability));
        }
        double hardPenalty = rating == Rating.HARD ? W[15] : 1.0;
        double easyBonus = rating == Rating.EASY ? W[16] : 1.0;
        return stability * (1
                + Math.exp(W[8])
                * (11 - difficulty)
                * Math.pow(stability, -W[9])
                * (Math.exp(W[10] * (1 - retrievability)) - 1)
                * hardPenalty
                * easyBonus);
    }

    /** Days until retrievability decays to TARGET_RETENTION, given current stability. */
    long intervalDaysFromStability(double stability) {
        double days = (9 * stability) * (1 / TARGET_RETENTION - 1);
        return Math.max(1, Math.round(days));
    }

    /**
     * Schedules the next review for a card given the person's recall rating.
     *
     * @param state current card state (use CardState.fresh() for a brand-new card)
     * @param rating how well the person recalled the pattern
     * @param now    the instant this review happened
     */
    public ScheduleResult review(CardState state, Rating rating, Instant now) {
        if (state.reps() == 0) {
            double stability = initStability(rating);
            double difficulty = initDifficulty(rating);
            long intervalDays = intervalDaysFromStability(stability);
            return new ScheduleResult(
                    difficulty, stability, 1,
                    rating == Rating.AGAIN ? 1 : 0,
                    now, now.plus(Duration.ofDays(intervalDays))
            );
        }

        double elapsedDays = state.lastReview() == null
                ? 0
                : Math.max(0, Duration.between(state.lastReview(), now).toHours() / 24.0);

        double r = retrievability(elapsedDays, state.stability());
        double newStability = nextStability(state.difficulty(), state.stability(), r, rating);
        double newDifficulty = nextDifficulty(state.difficulty(), rating);
        long intervalDays = intervalDaysFromStability(newStability);

        return new ScheduleResult(
                newDifficulty,
                newStability,
                state.reps() + 1,
                state.lapses() + (rating == Rating.AGAIN ? 1 : 0),
                now,
                now.plus(Duration.ofDays(intervalDays))
        );
    }
}
