package com.example.plms.repository;

import com.example.plms.domain.BookInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookInfoRepository extends JpaRepository<BookInfo, Long> {
}
