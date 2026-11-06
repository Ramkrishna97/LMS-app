package com.example.lms.content;

import com.example.lms.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "contents")
public class Content {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String title;
    @Lob private String body;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ContentType type;
    @ManyToOne(optional=false) private User author;
    @Column(nullable=false) private boolean published;
    private Instant uploadedAt = Instant.now();
    private Integer quizQuestionCount; private Integer quizDurationMinutes; private String fileName; private String mediaUrl; private String fileContentType;
    @Lob @JsonIgnore private byte[] fileData;
    @Transient private long quizAttemptCount;
    @Transient private int quizMaxScore;
    protected Content() {}
    public Content(String title, String body, ContentType type, User author, boolean published) { this.title=title;this.body=body;this.type=type;this.author=author;this.published=published; }
    public Long getId(){return id;} public String getTitle(){return title;} public String getBody(){return body;} public ContentType getType(){return type;} public User getAuthor(){return author;} public boolean isPublished(){return published;} public Instant getUploadedAt(){return uploadedAt;} public Integer getQuizQuestionCount(){return quizQuestionCount;} public Integer getQuizDurationMinutes(){return quizDurationMinutes;} public String getFileName(){return fileName;} public String getMediaUrl(){return mediaUrl;} public boolean isFileAvailable(){return fileData!=null;}
    public long getQuizAttemptCount(){return quizAttemptCount;} public int getQuizMaxScore(){return quizMaxScore;}
    public void setQuizStats(long attempts,int maxScore){quizAttemptCount=attempts;quizMaxScore=maxScore;}
    public void setQuizDetails(int questions,int minutes){quizQuestionCount=questions;quizDurationMinutes=minutes;} public void setFileName(String v){fileName=v;} public void setMediaUrl(String v){mediaUrl=v;} public void setFile(String name,String type,byte[] data){fileName=name;fileContentType=type;fileData=data;} public byte[] fileData(){return fileData;} public String fileContentType(){return fileContentType;}
    public void update(String title,String body,boolean published){this.title=title;this.body=body;this.published=published;}
}

