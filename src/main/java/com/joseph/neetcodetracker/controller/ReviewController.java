package com.joseph.neetcodetracker.controller;

import com.joseph.neetcodetracker.dto.ProblemDtos.ProblemResponse;
import com.joseph.neetcodetracker.dto.ProblemDtos.RateRequest;
import com.joseph.neetcodetracker.dto.ProblemDtos.SolveRequest;
import com.joseph.neetcodetracker.entity.ReviewCard;
import com.joseph.neetcodetracker.entity.User;
import com.joseph.neetcodetracker.repository.UserRepository;
import com.joseph.neetcodetracker.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername()).orElseThrow();
    }

    @GetMapping("/due")
    public List<ProblemResponse> due(@AuthenticationPrincipal UserDetails principal) {
        return reviewService.findDueForUser(currentUser(principal)).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{problemId}/solve")
    public ProblemResponse solve(@AuthenticationPrincipal UserDetails principal,
                                  @PathVariable Long problemId,
                                  @Valid @RequestBody SolveRequest req) {
        ReviewCard card = reviewService.markSolved(currentUser(principal), problemId, req.note());
        return toResponse(card);
    }

    @PatchMapping("/{problemId}/note")
    public ProblemResponse updateNote(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long problemId,
                                       @Valid @RequestBody SolveRequest req) {
        ReviewCard card = reviewService.updateNote(currentUser(principal), problemId, req.note());
        return toResponse(card);
    }

    @PostMapping("/{problemId}/rate")
    public ProblemResponse rate(@AuthenticationPrincipal UserDetails principal,
                                 @PathVariable Long problemId,
                                 @Valid @RequestBody RateRequest req) {
        ReviewCard card = reviewService.rate(currentUser(principal), problemId, req.rating());
        return toResponse(card);
    }

    private ProblemResponse toResponse(ReviewCard card) {
        return new ProblemResponse(
                card.getProblem().getId(),
                card.getProblem().getCategory(),
                card.getProblem().getName(),
                card.getStatus(),
                card.getNote(),
                card.getDue(),
                card.getReps(),
                card.getLapses()
        );
    }
}
