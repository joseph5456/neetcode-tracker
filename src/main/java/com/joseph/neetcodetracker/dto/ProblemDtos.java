package com.joseph.neetcodetracker.dto;

import com.joseph.neetcodetracker.entity.ReviewCard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ProblemDtos {

    public record ProblemResponse(
            Long problemId,
            String category,
            String name,
            ReviewCard.Status status,
            String note,
            Instant due,
            int reps,
            int lapses
    ) {}

    public record SolveRequest(
            @Size(max = 2000) String note
    ) {}

    public record RateRequest(
            @NotNull @Min(1) @Max(4) Integer rating // 1=Again 2=Hard 3=Good 4=Easy
    ) {}
}
