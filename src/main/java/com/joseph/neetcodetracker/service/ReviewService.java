package com.joseph.neetcodetracker.service;

import com.joseph.neetcodetracker.entity.Problem;
import com.joseph.neetcodetracker.entity.ReviewCard;
import com.joseph.neetcodetracker.entity.User;
import com.joseph.neetcodetracker.repository.ProblemRepository;
import com.joseph.neetcodetracker.repository.ReviewCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReviewService {

    private final ReviewCardRepository reviewCardRepository;
    private final ProblemRepository problemRepository;
    private final FsrsService fsrsService;

    public ReviewService(ReviewCardRepository reviewCardRepository,
                          ProblemRepository problemRepository,
                          FsrsService fsrsService) {
        this.reviewCardRepository = reviewCardRepository;
        this.problemRepository = problemRepository;
        this.fsrsService = fsrsService;
    }

    public List<ReviewCard> findAllForUser(User user) {
        return reviewCardRepository.findAllByUser(user);
    }

    public List<ReviewCard> findDueForUser(User user) {
        return reviewCardRepository.findAllByUserAndStatusAndDueLessThanEqualOrderByDueAsc(
                user, ReviewCard.Status.REVIEW, Instant.now());
    }

    /**
     * Marks a problem solved for the first time: seeds the FSRS card with a
     * "Good" rating so it enters the review queue, and stores the pattern note.
     */
    @Transactional
    public ReviewCard markSolved(User user, Long problemId, String note) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem not found: " + problemId));

        ReviewCard card = reviewCardRepository.findByUserAndProblemId(user, problemId)
                .orElseGet(() -> {
                    ReviewCard c = new ReviewCard();
                    c.setUser(user);
                    c.setProblem(problem);
                    return c;
                });

        if (card.getReps() == 0) {
            FsrsService.ScheduleResult result = fsrsService.review(
                    FsrsService.CardState.fresh(), FsrsService.Rating.GOOD, Instant.now());
            applySchedule(card, result);
        }
        card.setNote(note);
        card.setStatus(ReviewCard.Status.REVIEW);
        return reviewCardRepository.save(card);
    }

    /** Updates just the note on an already-solved card. */
    @Transactional
    public ReviewCard updateNote(User user, Long problemId, String note) {
        ReviewCard card = reviewCardRepository.findByUserAndProblemId(user, problemId)
                .orElseThrow(() -> new NoSuchElementException("Card not found for problem: " + problemId));
        card.setNote(note);
        return reviewCardRepository.save(card);
    }

    /** Applies a recall rating and reschedules the card via FSRS. */
    @Transactional
    public ReviewCard rate(User user, Long problemId, int ratingValue) {
        ReviewCard card = reviewCardRepository.findByUserAndProblemId(user, problemId)
                .orElseThrow(() -> new NoSuchElementException("Card not found for problem: " + problemId));

        FsrsService.CardState state = new FsrsService.CardState(
                card.getDifficulty(), card.getStability(), card.getReps(), card.getLapses(), card.getLastReview());

        FsrsService.Rating rating = switch (ratingValue) {
            case 1 -> FsrsService.Rating.AGAIN;
            case 2 -> FsrsService.Rating.HARD;
            case 3 -> FsrsService.Rating.GOOD;
            case 4 -> FsrsService.Rating.EASY;
            default -> throw new IllegalArgumentException("Rating must be 1-4");
        };

        FsrsService.ScheduleResult result = fsrsService.review(state, rating, Instant.now());
        applySchedule(card, result);
        return reviewCardRepository.save(card);
    }

    private void applySchedule(ReviewCard card, FsrsService.ScheduleResult result) {
        card.setDifficulty(result.difficulty());
        card.setStability(result.stability());
        card.setReps(result.reps());
        card.setLapses(result.lapses());
        card.setLastReview(result.lastReview());
        card.setDue(result.due());
    }
}
