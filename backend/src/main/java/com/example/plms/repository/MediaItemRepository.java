package com.example.plms.repository;

import com.example.plms.domain.MediaItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
    @Override
    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    List<MediaItem> findAll();

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    List<MediaItem> findAllByOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    Optional<MediaItem> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    Optional<MediaItem> findByIdAndDeletedAtIsNullAndOwner_Id(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    Optional<MediaItem> findById(Long id);

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    Optional<MediaItem> findByIdAndOwner_Id(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"tags"})
    List<MediaItem> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"tags"})
    List<MediaItem> findAllByDeletedAtIsNullAndOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"tags"})
    List<MediaItem> findAllByDeletedAtIsNotNull();

    @EntityGraph(attributePaths = {"tags"})
    List<MediaItem> findAllByDeletedAtIsNotNullAndOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    List<MediaItem> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"tags", "bookInfo", "bookInfo.authors", "dvdInfo", "dvdInfo.cast", "externalLinks"})
    List<MediaItem> findByIdInAndOwner_Id(List<Long> ids, Long ownerId);
}
