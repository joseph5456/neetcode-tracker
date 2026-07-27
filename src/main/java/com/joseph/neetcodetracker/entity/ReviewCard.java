package com.joseph.neetcodetracker.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "review_card", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"}))
public class ReviewCard {

    public enum Status { NEW, LEARNING, REVIEW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    @Column(length = 2000)
    private String note;

    // FSRS state
    private double difficulty;
    private double stability;
    private int reps;
    private int lapses;

    private Instant lastReview;
    private Instant due;

    public ReviewCard() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getDifficulty() { return difficulty; }
    public void setDifficulty(double difficulty) { this.difficulty = difficulty; }

    public double getStability() { return stability; }
    public void setStability(double stability) { this.stability = stability; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public int getLapses() { return lapses; }
    public void setLapses(int lapses) { this.lapses = lapses; }

    public Instant getLastReview() { return lastReview; }
    public void setLastReview(Instant lastReview) { this.lastReview = lastReview; }

    public Instant getDue() { return due; }
    public void setDue(Instant due) { this.due = due; }
}
