package pl.edu.agh.notification_service.Models;

public record MailStructure (
        String to,
        String subject,
        String message
) {}
