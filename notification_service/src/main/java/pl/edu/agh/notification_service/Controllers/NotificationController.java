package pl.edu.agh.notification_service.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.notification_service.CustomMailSender;
import pl.edu.agh.notification_service.Models.MailStructure;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final CustomMailSender mailSender;

    @Autowired
    public NotificationController(CustomMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostMapping("/sendMail")
    public ResponseEntity<String> SendMail(@RequestBody MailStructure mailStructure) {
        try {
            mailSender.sendEmail(mailStructure.to(), mailStructure.subject(), mailStructure.message());
        } catch (MailException ex) {
            return ResponseEntity.badRequest().body("Failed while sending the mail: " + ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
