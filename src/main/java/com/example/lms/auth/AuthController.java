package com.example.lms.auth;
import com.example.lms.user.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;} record Login(String username,String password){} record Register(String name,String email,String username,String password,Role role){} record Forgot(String email){} record Password(String password){}
 @PostMapping("/login") public AuthService.AuthResponse login(@RequestBody Login r){return auth.login(r.username,r.password);}
 @PostMapping("/register") public User register(@RequestBody Register r){return auth.register(r.name,r.email,r.username,r.password,r.role);}
 @PostMapping("/forgot-password") public Map<String,String> forgot(@RequestBody Forgot r){return Map.of("temporaryPassword",auth.reset(r.email));}
 @GetMapping("/me") public User me(@RequestHeader("X-Auth-Token") String token){return auth.current(token);}
 @PostMapping("/password") public void password(@RequestHeader("X-Auth-Token") String token,@RequestBody Password r){auth.changePassword(auth.current(token),r.password);}
 @PostMapping("/logout") public void logout(@RequestHeader("X-Auth-Token") String token){auth.logout(token);}
}
