package com.fiverr.demo.repository;

import com.fiverr.demo.dto.MonthlyClickStats;
import com.fiverr.demo.entity.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {
    long countByLinkIdAndIsValidTrue(Long linkId);

    @Query("""
        SELECT TO_CHAR(c.clickedAt, 'YYYY-MM') as month, COUNT(c.id) as clickCount
        FROM Click c
        WHERE c.link.id = :linkId AND c.isValid = true
        GROUP BY TO_CHAR(c.clickedAt, 'YYYY-MM')
        ORDER BY month DESC
        """)
    List<MonthlyClickStats> getMonthlyStats(@Param("linkId") Long linkId);
}
