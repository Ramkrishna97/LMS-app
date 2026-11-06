package com.example.lms.content;

import com.example.lms.auth.AuthService;
import com.example.lms.quiz.QuizAttemptRepository;
import com.example.lms.quiz.QuizRepository;
import com.example.lms.user.Role;
import com.example.lms.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ContentController {
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx", "txt");
    private final ContentRepository repository;
    private final AuthService auth;
    private final ImageProcessingService images;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;

    public ContentController(ContentRepository repository, AuthService auth, ImageProcessingService images,
                             QuizRepository quizzes, QuizAttemptRepository attempts) {
        this.repository = repository;
        this.auth = auth;
        this.images = images;
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    record ContentRequest(String title, String body, ContentType type, String fileName, String mediaUrl) {}

    @GetMapping("/dashboard")
    public List<Content> dashboard() {
        List<Content> feed = repository.findByPublishedTrueOrderByUploadedAtDesc();
        Map<Long, Content> byId = new java.util.HashMap<>();
        feed.stream().filter(item -> item.getType() == ContentType.QUIZ).forEach(item -> byId.put(item.getId(), item));
        for (Object[] stat : attempts.contentStats()) {
            Content item = byId.get(((Number) stat[0]).longValue());
            if (item != null) item.setQuizStats(((Number) stat[1]).longValue(), ((Number) stat[2]).intValue());
        }
        return feed;
    }

    @GetMapping("/contents/manage")
    public Page<Content> manage(@RequestHeader("X-Auth-Token") String token,
                                @RequestParam(required = false) Long authorId,
                                @RequestParam(required = false) ContentType type,
                                @RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to,
                                @RequestParam(required = false) String q,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        if (auth.current(token).getRole() != Role.ADMIN) throw new IllegalArgumentException("Admin access required");
        return filteredContents(authorId, type, from, to, q, page, size);
    }

    @GetMapping("/contents/mine")
    public Page<Content> mine(@RequestHeader("X-Auth-Token") String token,
                              @RequestParam(required = false) ContentType type,
                              @RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              @RequestParam(required = false) String q,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        User teacher = publisher(token);
        return filteredContents(teacher.getId(), type, from, to, q, page, size);
    }

    private Page<Content> filteredContents(Long authorId, ContentType type, LocalDate from, LocalDate to,
                                           String q, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page size or number");
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("From date must precede to date");
        Specification<Content> filter = (root, query, builder) -> builder.conjunction();
        if (authorId != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("author").get("id"), authorId));
        if (type != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("type"), type));
        if (from != null) {
            Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            filter = filter.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("uploadedAt"), start));
        }
        if (to != null) {
            Instant end = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            filter = filter.and((root, query, builder) -> builder.lessThan(root.get("uploadedAt"), end));
        }
        if (q != null && !q.isBlank()) {
            String search = "%" + q.toLowerCase().trim() + "%";
            filter = filter.and((root, query, builder) -> builder.like(builder.lower(root.get("title")), search));
        }
        return repository.findAll(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt", "id")));
    }

    @GetMapping("/contents/{id}")
    public ResponseEntity<Content> get(@PathVariable Long id) {
        return repository.findById(id).filter(Content::isPublished).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/contents")
    public Content create(@RequestHeader("X-Auth-Token") String token, @RequestBody ContentRequest request) {
        User author = publisher(token);
        validateText(request.title(), "Title");
        if (request.type() != ContentType.POST) throw new IllegalArgumentException("Use file upload for documents/images and quiz import for quizzes");
        return repository.save(new Content(request.title().trim(), request.body(), ContentType.POST, author, true));
    }

    @PutMapping("/contents/{id}")
    public Content update(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @RequestBody ContentRequest request) {
        User user = publisher(token);
        Content content = repository.findById(id).orElseThrow();
        checkOwner(user, content);
        validateText(request.title(), "Title");
        content.update(request.title().trim(), request.body(), true);
        return repository.save(content);
    }

    @DeleteMapping("/contents/{id}")
    @Transactional
    public void delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        User user = publisher(token);
        Content content = repository.findById(id).orElseThrow();
        checkOwner(user, content);
        if (content.getType() == ContentType.QUIZ) {
            quizzes.findByContentId(id).ifPresent(quiz -> {
                attempts.deleteByQuizId(quiz.getId());
                attempts.flush();
                quizzes.delete(quiz);
                quizzes.flush();
            });
        }
        repository.delete(content);
    }

    @PostMapping(value = "/contents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Content upload(@RequestHeader("X-Auth-Token") String token, @RequestParam String title,
                          @RequestParam String description, @RequestParam ContentType type,
                          @RequestParam MultipartFile file) throws IOException {
        User author = publisher(token);
        validateText(title, "Title");
        if (type != ContentType.DOCUMENT && type != ContentType.IMAGE)
            throw new IllegalArgumentException("Uploads must be DOCUMENT or IMAGE content");
        if (file.isEmpty()) throw new IllegalArgumentException("Choose a file to upload");
        Content content = new Content(title.trim(), description, type, author, true);
        if (type == ContentType.IMAGE) {
            var image = images.normalize(file);
            content.setFile(image.name(), image.contentType(), image.data());
        } else {
            String name = safeName(file.getOriginalFilename());
            String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            if (!DOCUMENT_EXTENSIONS.contains(extension))
                throw new IllegalArgumentException("Supported documents: PDF, Word, Excel, and TXT");
            content.setFile(name, mediaTypeFor(extension), file.getBytes());
        }
        return repository.save(content);
    }

    @GetMapping("/contents/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) {
        Content content = repository.findById(id).filter(Content::isPublished).orElse(null);
        if (content == null || content.fileData() == null) return ResponseEntity.notFound().build();
        String name = safeName(content.getFileName());
        boolean inline = content.getType() == ContentType.IMAGE || name.toLowerCase().endsWith(".pdf");
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(name, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.fileContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.fileData());
    }

    private User publisher(String token) {
        User user = auth.current(token);
        if (user.getRole() == Role.STUDENT) throw new IllegalArgumentException("Only teachers and admins can publish");
        return user;
    }

    private void checkOwner(User user, Content content) {
        if (user.getRole() != Role.ADMIN && !content.getAuthor().getId().equals(user.getId()))
            throw new IllegalArgumentException("You can only change your own content");
    }

    private void validateText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
    }

    private String safeName(String original) {
        if (original == null || original.isBlank()) return "download";
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.replaceAll("[\\r\\n\";]", "_");
    }

    private String mediaTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "text/plain";
        };
    }
}
