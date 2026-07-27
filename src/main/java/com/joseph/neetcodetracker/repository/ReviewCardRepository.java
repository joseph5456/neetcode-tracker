package com.joseph.neetcodetracker.repository;

import com.joseph.neetcodetracker.entity.ReviewCard;
import com.joseph.neetcodetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewCardRepository extends JpaRepository<ReviewCard, Long> {

    Optional<ReviewCard> findByUserAndProblemId(User user, Long problemId);

    List<ReviewCard> findAllByUser(User user);

    List<ReviewCard> findAllByUserAndStatusAndDueLessThanEqualOrderByDueAsc(
            User user, ReviewCard.Status status, Instant now);
}
