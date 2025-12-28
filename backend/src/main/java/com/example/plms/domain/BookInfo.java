package com.example.plms.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "book_info")
public class BookInfo {
    @Id
    private Long itemId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "item_id")
    private MediaItem item;

    private String isbn;

    private Integer pages;

    private String publisher;

    private String authorsText;

    @ElementCollection
    @CollectionTable(name = "book_author", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "author")
    private Set<String> authors = new LinkedHashSet<>();

    protected BookInfo() {
    }

    public BookInfo(MediaItem item) {
        this.item = item;
    }

    public Long getItemId() {
        return itemId;
    }

    public MediaItem getItem() {
        return item;
    }

    public void setItem(MediaItem item) {
        this.item = item;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getAuthorsText() {
        return authorsText;
    }

    public void setAuthorsText(String authorsText) {
        this.authorsText = authorsText;
    }

    public Set<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors == null ? new LinkedHashSet<>() : new LinkedHashSet<>(authors);
    }
}
