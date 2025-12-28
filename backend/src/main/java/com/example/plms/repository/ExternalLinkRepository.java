package com.example.plms.repository;

import com.example.plms.domain.ExternalLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {
    List<ExternalLink> findByItemId(Long itemId);
}
