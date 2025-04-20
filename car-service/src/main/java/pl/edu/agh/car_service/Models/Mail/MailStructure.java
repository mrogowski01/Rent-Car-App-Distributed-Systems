package pl.edu.agh.car_service.Models.Mail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MailStructure {
    private String to;
    private String subject;
    private String message;
}
