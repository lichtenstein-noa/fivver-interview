package com.fiverr.demo.repository;

import com.fiverr.demo.entity.ShortenedLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortenedLinkRepository extends JpaRepository<ShortenedLink, Long> {
    Optional<ShortenedLink> findByTargetUrl(String targetUrl);
    Optional<ShortenedLink> findByShortCode(String shortCode);
}
