package com.fiverr.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clicks",
       indexes = {
           @Index(name = "idx_link_id", columnList = "link_id"),
           @Index(name = "idx_clicked_at", columnList = "clicked_at")
       })
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private ShortenedLink link;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;

    @Column(name = "earnings", nullable = false, precision = 10, scale = 2)
    private BigDecimal earnings = new BigDecimal("0.05");

    @PrePersist
    protected void onCreate() {
        clickedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShortenedLink getLink() {
        return link;
    }

    public void setLink(ShortenedLink link) {
        this.link = link;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public BigDecimal getEarnings() {
        return earnings;
    }

    public void setEarnings(BigDecimal earnings) {
        this.earnings = earnings;
    }
}
