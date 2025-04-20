package pl.edu.agh.car_service.Helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.edu.agh.car_service.Models.Mail.MailStructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MailHelper {
    @Value("${mail.notification.url}")
    private String URL;

    private final RestClient restClient;

    @Autowired
    public MailHelper(RestClient restClient) {
        this.restClient = restClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(MailHelper.class);

    public boolean sendMail(MailStructure mailStructure) {
        logger.info("Sending mail to: {}, subject: {}, message: {}",
                mailStructure.getTo(),
                mailStructure.getSubject(),
                mailStructure.getMessage());
        try {
            var response = restClient.post()
                    .uri(URL)
                    .body(mailStructure)
                    .retrieve()
                    .toEntity(String.class);
            logger.info("Mail sent successfully with status: {}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Failed to send mail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send mail: " + e.getMessage(), e);
        }
    }
}
