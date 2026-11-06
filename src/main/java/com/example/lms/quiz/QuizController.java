package com.example.lms.quiz;

import com.example.lms.auth.AuthService;
import com.example.lms.content.Content;
import com.example.lms.content.ContentRepository;
import com.example.lms.content.ContentType;
import com.example.lms.user.Role;
import com.example.lms.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final ContentRepository contents;
    private final AuthService auth;
    private final ObjectMapper mapper;

    public QuizController(QuizRepository quizzes, QuizAttemptRepository attempts, ContentRepository contents,
                          AuthService auth, ObjectMapper mapper) {
        this.quizzes = quizzes; this.attempts = attempts; this.contents = contents;
        this.auth = auth; this.mapper = mapper;
    }

    record Question(String questionText, String optionA, String optionB, String optionC, String optionD,
                    Integer correctOption, String solution) {}
    record Create(String title, String description, Integer durationMinutes, List<Question> questions) {}
    record Submit(Map<Long, Integer> answers) {}

    private User teacher(String token) {
        User user = auth.current(token);
        if (user.getRole() == Role.STUDENT) throw new IllegalArgumentException("Teacher or admin access required");
        return user;
    }

    private void validate(Create request) {
        if (blank(request.title()) || blank(request.description()) || request.durationMinutes() == null
                || request.durationMinutes() <= 0 || request.questions() == null || request.questions().isEmpty())
            throw new IllegalArgumentException("A quiz needs a title, description, positive duration, and at least one question");
        for (int index = 0; index < request.questions().size(); index++) {
            Question question = request.questions().get(index);
            if (question == null || blank(question.questionText()) || blank(question.optionA())
                    || blank(question.optionB()) || blank(question.optionC()) || blank(question.optionD())
                    || blank(question.solution()) || question.correctOption() == null
                    || question.correctOption() < 0 || question.correctOption() > 3)
                throw new IllegalArgumentException("Question " + (index + 1) + " needs text, four options, a solution, and correctOption 0-3");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestHeader("X-Auth-Token") String token, @RequestBody Create request) {
        User author = teacher(token);
        validate(request);
        Content content = new Content(request.title().trim(), request.description().trim(), ContentType.QUIZ, author, true);
        content.setQuizDetails(request.questions().size(), request.durationMinutes());
        contents.save(content);
        Quiz quiz = new Quiz(content, request.durationMinutes());
        request.questions().forEach(q -> quiz.add(new QuizQuestion(q.questionText().trim(), q.optionA().trim(),
                q.optionB().trim(), q.optionC().trim(), q.optionD().trim(), q.correctOption(), q.solution().trim())));
        quizzes.save(quiz);
        return summary(quiz);
    }

    @GetMapping("/content/{contentId}")
    public Map<String, Object> get(@PathVariable Long contentId) {
        return summary(quizzes.findByContentId(contentId).orElseThrow());
    }

    @PostMapping("/{id}/attempt")
    public Map<String, Object> submit(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
                                      @RequestBody Submit request) {
        User student = auth.current(token);
        if (student.getRole() != Role.STUDENT) throw new IllegalArgumentException("Only students can attempt quizzes");
        Quiz quiz = quizzes.findById(id).orElseThrow();
        Map<Long, Integer> answers = request.answers() == null ? Map.of() : request.answers();
        int score = (int) quiz.getQuestions().stream()
                .filter(q -> Objects.equals(answers.get(q.getId()), q.correct())).count();
        attempts.saveAndFlush(new QuizAttempt(quiz, student, score, quiz.getQuestions().size()));
        return Map.of("score", score, "total", quiz.getQuestions().size(),
                "attemptCount", attempts.countDistinctStudents(quiz.getId()),
                "maxScore", Optional.ofNullable(attempts.maxScore(quiz.getId())).orElse(0), "solutions",
                quiz.getQuestions().stream().map(q -> Map.of("question", q.getQuestionText(),
                        "correctOption", q.correct(), "solution", q.getSolution())).toList());
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Transactional
    public Map<String, Object> importQuiz(@RequestHeader("X-Auth-Token") String token,
                                          @RequestParam MultipartFile file) throws Exception {
        teacher(token);
        if (file.isEmpty() || !Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase().endsWith(".json"))
            throw new IllegalArgumentException("Choose a JSON quiz file");
        Create request;
        try { request = mapper.readValue(file.getBytes(), Create.class); }
        catch (Exception exception) { throw new IllegalArgumentException("Invalid quiz JSON schema: " + exception.getMessage()); }
        validate(request);
        return create(token, request);
    }

    private Map<String, Object> summary(Quiz quiz) {
        return Map.of("id", quiz.getId(), "contentId", quiz.getContent().getId(),
                "title", quiz.getContent().getTitle(), "description", quiz.getContent().getBody(),
                "durationMinutes", quiz.getDurationMinutes(), "questionCount", quiz.getQuestions().size(),
                "attemptCount", attempts.countDistinctStudents(quiz.getId()),
                "maxScore", Optional.ofNullable(attempts.maxScore(quiz.getId())).orElse(0),
                "questions", quiz.getQuestions());
    }
}
