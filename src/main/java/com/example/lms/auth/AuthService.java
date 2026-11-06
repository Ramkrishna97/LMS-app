package com.example.lms.auth;

import com.example.lms.user.Role;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, AuthSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    public record AuthResponse(String token, User user) {}

    public AuthResponse login(String username, String password) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (password == null || !user.password().equals(password))
            throw new IllegalArgumentException("Invalid username or password");
        String token = UUID.randomUUID().toString();
        sessions.save(new AuthSession(token, user, Instant.now().plus(30, ChronoUnit.DAYS)));
        return new AuthResponse(token, user);
    }

    public User current(String token) {
        AuthSession session = sessions.findById(token == null ? "" : token)
                .orElseThrow(() -> new IllegalArgumentException("Please sign in"));
        if (session.getExpiresAt().isBefore(Instant.now())) {
            sessions.delete(session);
            throw new IllegalArgumentException("Please sign in");
        }
        return session.getUser();
    }

    public void logout(String token) { if (token != null) sessions.deleteById(token); }

    public User register(String name, String email, String username, String password, Role role) {
        if (role == null || role == Role.ADMIN) throw new IllegalArgumentException("Choose student or teacher");
        if (users.findByUsername(username).isPresent() || users.findByEmail(email).isPresent())
            throw new IllegalArgumentException("Username or email is already registered");
        return users.save(new User(name, email, username, password, role));
    }

    @Transactional
    public String reset(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No account found for that email"));
        String password = "LMS" + (100000 + random.nextInt(900000));
        user.setPassword(password);
        sessions.deleteByUserId(user.getId());
        users.save(user);
        return password;
    }

    public void changePassword(User user, String password) {
        user.setPassword(password);
        users.save(user);
    }
}
