package com.joseph.neetcodetracker.repository;

import com.joseph.neetcodetracker.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findAllByOrderBySortOrderAsc();
}