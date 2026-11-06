package com.example.lms.config;
import com.example.lms.content.*; import com.example.lms.quiz.*; import org.springframework.boot.ApplicationRunner; import org.springframework.context.annotation.*; import org.springframework.core.annotation.Order;
@Configuration public class QuizDataRepair {
 @Bean @Order(100) ApplicationRunner repairQuizCounts(ContentRepository contents,QuizRepository quizzes){return args->contents.findByPublishedTrueOrderByUploadedAtDesc().stream().filter(c->c.getType()==ContentType.QUIZ).forEach(c->quizzes.findByContentId(c.getId()).ifPresent(q->{c.setQuizDetails(q.getQuestions().size(),q.getDurationMinutes());contents.save(c);}));}
}
