package com.joseph.neetcodetracker.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FsrsServiceTest {

    private final FsrsService fsrs = new FsrsService();
    private final Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void freshCard_firstReview_seedsStabilityAndSchedulesFutureDue() {
        FsrsService.ScheduleResult result = fsrs.review(FsrsService.CardState.fresh(), FsrsService.Rating.GOOD, t0);

        assertEquals(1, result.reps());
        assertEquals(0, result.lapses());
        assertTrue(result.due().isAfter(t0));
        assertTrue(result.stability() > 0);
    }

    @Test
    void repeatedGoodRatings_growTheInterval() {
        FsrsService.ScheduleResult first = fsrs.review(FsrsService.CardState.fresh(), FsrsService.Rating.GOOD, t0);
        long firstIntervalDays = Duration.between(t0, first.due()).toDays();

        FsrsService.CardState afterFirst = new FsrsService.CardState(
                first.difficulty(), first.stability(), first.reps(), first.lapses(), first.lastReview());

        Instant secondReviewTime = first.due(); // review right when it's due
        FsrsService.ScheduleResult second = fsrs.review(afterFirst, FsrsService.Rating.GOOD, secondReviewTime);
        long secondIntervalDays = Duration.between(secondReviewTime, second.due()).toDays();

        assertTrue(secondIntervalDays > firstIntervalDays,
                "interval should grow after a second consecutive Good rating");
    }

    @Test
    void againRating_shrinksIntervalAndIncrementsLapses() {
        FsrsService.ScheduleResult first = fsrs.review(FsrsService.CardState.fresh(), FsrsService.Rating.GOOD, t0);
        FsrsService.CardState afterFirst = new FsrsService.CardState(
                first.difficulty(), first.stability(), first.reps(), first.lapses(), first.lastReview());

        Instant secondReviewTime = first.due().plus(Duration.ofDays(10)); // reviewed late
        FsrsService.ScheduleResult afterLapse = fsrs.review(afterFirst, FsrsService.Rating.AGAIN, secondReviewTime);

        assertEquals(1, afterLapse.lapses());
        long lapseInterval = Duration.between(secondReviewTime, afterLapse.due()).toDays();
        assertTrue(lapseInterval <= 2, "an 'Again' rating should schedule a near-term review");
    }

    @Test
    void easyRating_schedulesFurtherOutThanGood_onFirstReview() {
        FsrsService.ScheduleResult good = fsrs.review(FsrsService.CardState.fresh(), FsrsService.Rating.GOOD, t0);
        FsrsService.ScheduleResult easy = fsrs.review(FsrsService.CardState.fresh(), FsrsService.Rating.EASY, t0);

        assertTrue(easy.due().isAfter(good.due()),
                "Easy should push the due date further out than Good on a first review");
    }

    @Test
    void difficultyStaysWithinPublishedBounds() {
        FsrsService.ScheduleResult result = fsrs.review(FsrsService.CardState.fresh(), FsrsService.Rating.AGAIN, t0);
        assertTrue(result.difficulty() >= 1 && result.difficulty() <= 10);
    }
}
