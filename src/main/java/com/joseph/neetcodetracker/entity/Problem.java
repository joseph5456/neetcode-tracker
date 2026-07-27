package com.joseph.neetcodetracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "problem", uniqueConstraints = @UniqueConstraint(columnNames = {"category", "name"}))
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String name;

    // Order within its category, mirrors NeetCode's recommended sequence
    @Column(nullable = false)
    private Integer sortOrder;

    public Problem() {}

    public Problem(String category, String name, Integer sortOrder) {
        this.category = category;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
