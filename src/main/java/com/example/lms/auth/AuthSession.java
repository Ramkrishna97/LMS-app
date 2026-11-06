package com.example.lms.auth;

import com.example.lms.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class AuthSession {
    @Id private String token;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) private User user;
    private Instant expiresAt;

    protected AuthSession() {}
    public AuthSession(String token, User user, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}
