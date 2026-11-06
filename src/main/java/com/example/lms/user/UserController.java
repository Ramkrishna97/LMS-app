package com.example.lms.user;
import com.example.lms.auth.AuthService; import com.example.lms.auth.AuthSessionRepository; import com.example.lms.content.*; import com.example.lms.quiz.*; import jakarta.transaction.Transactional; import javax.imageio.*; import javax.imageio.stream.ImageOutputStream; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.awt.image.*; import java.io.*; import java.util.*;
@RestController @RequestMapping("/api/users") public class UserController {
 private final UserRepository users; private final AuthService auth; private final AuthSessionRepository sessions; private final ContentRepository contents; private final QuizRepository quizzes; private final QuizAttemptRepository attempts;
 public UserController(UserRepository users,AuthService auth,AuthSessionRepository sessions,ContentRepository contents,QuizRepository quizzes,QuizAttemptRepository attempts){this.users=users;this.auth=auth;this.sessions=sessions;this.contents=contents;this.quizzes=quizzes;this.attempts=attempts;} record Profile(String name,Integer age,String bio,String phone){}
 private User current(String t){return auth.current(t);} private void admin(String t){if(current(t).getRole()!=Role.ADMIN)throw new IllegalArgumentException("Admin access required");}
 @PutMapping("/me") public User update(@RequestHeader("X-Auth-Token") String t,@RequestBody Profile p){User u=current(t);u.update(p.name,p.age,p.bio,p.phone);return users.save(u);}
 @GetMapping public List<User> all(@RequestHeader("X-Auth-Token") String t){admin(t);return users.findAll();}
 @PostMapping public User add(@RequestHeader("X-Auth-Token") String t,@RequestBody Map<String,Object> p){admin(t);Role role=Role.valueOf(String.valueOf(p.get("role")));if(role==Role.ADMIN)throw new IllegalArgumentException("Only one admin is allowed");return auth.register(String.valueOf(p.get("name")),String.valueOf(p.get("email")),String.valueOf(p.get("username")),String.valueOf(p.get("password")),role);}
 @PutMapping("/{id}") public User updateUser(@RequestHeader("X-Auth-Token") String t,@PathVariable Long id,@RequestBody Profile p){admin(t);User u=users.findById(id).orElseThrow();u.update(p.name,p.age,p.bio,p.phone);return users.save(u);}
 @DeleteMapping("/{id}") @Transactional public void delete(@RequestHeader("X-Auth-Token") String t,@PathVariable Long id){
  admin(t);User user=users.findById(id).orElseThrow();
  if(user.getRole()==Role.ADMIN)throw new IllegalArgumentException("The admin account cannot be deleted");
  for(Content content:contents.findByAuthorId(id)){
   if(content.getType()==ContentType.QUIZ)quizzes.findByContentId(content.getId()).ifPresent(quiz->{
    attempts.deleteByQuizId(quiz.getId());attempts.flush();quizzes.delete(quiz);quizzes.flush();
   });
   contents.delete(content);
  }
  contents.flush();
  attempts.deleteByStudentId(id);attempts.flush();
  sessions.deleteByUserId(id);sessions.flush();
  users.delete(user);
 }
 @PostMapping("/me/photo") public User photo(@RequestHeader("X-Auth-Token") String t,@RequestParam MultipartFile file) throws IOException {
  User user=current(t);
  BufferedImage original=ImageIO.read(file.getInputStream());
  if(original==null)throw new IllegalArgumentException("Please upload an image file");
  byte[] result=null;
  int max=160;
  for(int size=max;size>=48 && result==null;size=(int)(size*0.8)){
   double scale=Math.min(1d,(double)size/Math.max(original.getWidth(),original.getHeight()));
   BufferedImage out=new BufferedImage(Math.max(1,(int)(original.getWidth()*scale)),Math.max(1,(int)(original.getHeight()*scale)),BufferedImage.TYPE_INT_RGB);
   var graphics=out.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,out.getWidth(),out.getHeight());
   graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
   graphics.drawImage(original,0,0,out.getWidth(),out.getHeight(),null);graphics.dispose();
   for(float quality=0.85f;quality>=0.4f;quality-=0.15f){
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
    try(ImageOutputStream stream=ImageIO.createImageOutputStream(bytes)){
     writer.setOutput(stream);ImageWriteParam params=writer.getDefaultWriteParam();
     params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(quality);
     writer.write(null,new IIOImage(out,null,null),params);
    }finally{writer.dispose();}
    if(bytes.size()<=50*1024){result=bytes.toByteArray();break;}
   }
  }
  if(result==null)throw new IllegalArgumentException("Image could not be reduced below 50 KB");
  user.setPhoto(result,"image/jpeg");return users.save(user);
 }
 @GetMapping("/{id}/photo") public ResponseEntity<byte[]> getPhoto(@PathVariable Long id){User u=users.findById(id).orElseThrow();return u.photo()==null?ResponseEntity.notFound().build():ResponseEntity.ok().contentType(MediaType.parseMediaType(u.photoType())).body(u.photo());}
}
