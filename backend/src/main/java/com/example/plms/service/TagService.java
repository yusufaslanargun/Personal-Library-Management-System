package com.example.plms.service;

import com.example.plms.domain.Tag;
import com.example.plms.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Set<Tag> resolveTags(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new HashSet<>();
        }
        Set<Tag> tags = new HashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Tag tag = tagRepository.findByNameIgnoreCase(name.trim())
                .orElseGet(() -> tagRepository.save(new Tag(name.trim())));
            tags.add(tag);
        }
        return tags;
    }
}
