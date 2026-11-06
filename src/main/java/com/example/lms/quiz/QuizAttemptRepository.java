package com.example.lms.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    @Query("select count(distinct a.student.id) from QuizAttempt a where a.quiz.id = :id")
    long countDistinctStudents(Long id);

    @Query("select max(a.score) from QuizAttempt a where a.quiz.id = :id")
    Integer maxScore(Long id);

    @Query("select a.quiz.content.id, count(distinct a.student.id), max(a.score) from QuizAttempt a group by a.quiz.content.id")
    List<Object[]> contentStats();

    void deleteByQuizId(Long id);
    void deleteByStudentId(Long id);
}
