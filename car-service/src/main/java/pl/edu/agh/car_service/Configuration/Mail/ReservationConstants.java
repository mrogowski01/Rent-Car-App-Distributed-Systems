package pl.edu.agh.car_service.Configuration.Mail;

public class ReservationConstants {
    public static String reservationMailTitleCreate = "Reservation created";
    public static String reservationMailContentCreate = "Your reservation has been created!\nYour car %s %s from %s - %s\nReservation price: %s PLN";
    public static String ownerReservationMailContentCreate = "User with email %s has created reservation your car %s %s with id - %s in " +
            "offer: price - %s, start date - %s, end date - %s. \nReservation from %s to %s price - %s PLN";

    public static String reservationMailTitleDelete = "Reservation deleted";
    public static String reservationMailContentDelete = "Your reservation has been deleted!\nYour car %s %s from %s - %s\nReservation price: %s PLN";
    public static String ownerReservationMailContentDelete = "User with email %s has deleted reservation for your car %s %s with id - %s in " +
            "offer: price - %s, start date - %s, end date - %s. \nReservation from %s to %s price - %s PLN";

    public static String reservationMailTitleUpdate = "Reservation updated";
    public static String reservationMailContentUpdate = "Your reservation has been updated!\nYour car %s %s. Previous dates: from %s - %s, current: from %s - %s\nReservation price: %s PLN";
    public static String ownerReservationMailContentUpdate = "User with email %s has updated reservation for your car %s %s with id - %s in " +
            "offer: price - %s, start date - %s, end date - %s. \nPrevious reservation from: %s to %s, current reservation from: %s to %s current price - %s PLN";
}
