package com.example.lms.content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface ContentRepository extends JpaRepository<Content, Long>, JpaSpecificationExecutor<Content> {
    List<Content> findByPublishedTrueOrderByUploadedAtDesc();
    List<Content> findByAuthorId(Long authorId);
}

