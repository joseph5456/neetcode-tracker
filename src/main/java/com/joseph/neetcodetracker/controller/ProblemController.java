package com.joseph.neetcodetracker.controller;

import com.joseph.neetcodetracker.dto.ProblemDtos.ProblemResponse;
import com.joseph.neetcodetracker.entity.Problem;
import com.joseph.neetcodetracker.entity.ReviewCard;
import com.joseph.neetcodetracker.entity.User;
import com.joseph.neetcodetracker.repository.ProblemRepository;
import com.joseph.neetcodetracker.repository.ReviewCardRepository;
import com.joseph.neetcodetracker.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemRepository problemRepository;
    private final ReviewCardRepository reviewCardRepository;
    private final UserRepository userRepository;

    public ProblemController(ProblemRepository problemRepository,
                              ReviewCardRepository reviewCardRepository,
                              UserRepository userRepository) {
        this.problemRepository = problemRepository;
        this.reviewCardRepository = reviewCardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Public list of the NeetCode 150 catalog. When called with a valid JWT,
     * each entry is merged with that user's own review state.
     */
    @GetMapping
    public List<ProblemResponse> listProblems(@AuthenticationPrincipal UserDetails principal) {
        List<Problem> problems = problemRepository.findAllByOrderByCategoryAscSortOrderAsc();

        Map<Long, ReviewCard> cardsByProblemId = new HashMap<>();
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
            reviewCardRepository.findAllByUser(user)
                    .forEach(c -> cardsByProblemId.put(c.getProblem().getId(), c));
        }

        return problems.stream().map(p -> {
            ReviewCard card = cardsByProblemId.get(p.getId());
            return new ProblemResponse(
                    p.getId(), p.getCategory(), p.getName(),
                    card != null ? card.getStatus() : ReviewCard.Status.NEW,
                    card != null ? card.getNote() : null,
                    card != null ? card.getDue() : null,
                    card != null ? card.getReps() : 0,
                    card != null ? card.getLapses() : 0
            );
        }).toList();
    }
}
