package com.example.plms.repository;

import com.example.plms.domain.MediaList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaListRepository extends JpaRepository<MediaList, Long> {
    List<MediaList> findAllByOwner_Id(Long ownerId);
    Optional<MediaList> findByIdAndOwner_Id(Long id, Long ownerId);
}
