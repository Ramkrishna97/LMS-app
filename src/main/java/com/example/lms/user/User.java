package com.example.lms.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "platform_users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(unique = true) private String username;
    @JsonIgnore private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    private Integer age; private String bio; private String phone;
    @Lob @JsonIgnore private byte[] profilePhoto;
    @JsonIgnore private String profilePhotoType;
    private Instant createdAt = Instant.now();
    protected User() {}
    public User(String name, String email, String username, String password, Role role) { this.name=name; this.email=email; this.username=username; this.password=password; this.role=role; }
    public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getUsername(){return username;} public Role getRole(){return role;} public Integer getAge(){return age;} public String getBio(){return bio;} public String getPhone(){return phone;} public Instant getCreatedAt(){return createdAt;}
    public void update(String name, Integer age, String bio, String phone){this.name=name;this.age=age;this.bio=bio;this.phone=phone;} public void setPassword(String password){this.password=password;} public void setCredentials(String username,String password){this.username=username;this.password=password;} public String password(){return password;} public byte[] photo(){return profilePhoto;} public String photoType(){return profilePhotoType;} public void setPhoto(byte[] photo,String type){profilePhoto=photo;profilePhotoType=type;}
}

