package pl.edu.agh.car_service.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Models.Reservation.AddReservationDto;
import pl.edu.agh.car_service.Models.Reservation.ReservationDto;
import pl.edu.agh.car_service.Models.Response.ResponseMessage;
import pl.edu.agh.car_service.Services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/reservations")
public class ReservationController {
    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);
    private final ReservationService reservationService;
    private final JwtUtils jwtUtils;

    @Autowired
    public ReservationController(ReservationService reservationService, JwtUtils jwtUtils) {
        this.reservationService = reservationService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestHeader("Authorization") String token, @RequestBody AddReservationDto reservation) {
        try {
            Long userId = jwtUtils.getUserId(token);
            if (userId == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            String email = jwtUtils.getUserMail(token);

            Long reservationId = reservationService.createReservation(reservation, userId, email, token);
            return ResponseEntity.ok().body(reservationId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Error creating reservation: " + e.getMessage()));
        }
    }

    @PutMapping("/{idReservation}")
    public ResponseEntity<ResponseMessage> updateReservation(@PathVariable Long idReservation, @RequestHeader("Authorization") String token, @RequestBody AddReservationDto reservation) {
        try {
            Long userId = jwtUtils.getUserId(token);
            String role = jwtUtils.getRole(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            reservationService.updateReservation(idReservation, reservation, userId, role, token);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseMessage(400, e.getMessage()));
        }
    }

    @GetMapping("/{idReservation}")
    public ResponseEntity<ReservationDto> getReservationById(@PathVariable Long idReservation, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var reservation = reservationService.getReservationById(idReservation);
        if (reservation == null) return ResponseEntity.notFound().build();
        if (!Objects.equals(reservation.getIdUser(), userId))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(reservation);
    }

    @DeleteMapping("/{idReservation}")
    public ResponseEntity<ResponseMessage> deleteReservation(@RequestHeader("Authorization") String token, @PathVariable Long idReservation) {
        try {
            Long userId = jwtUtils.getUserId(token);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            var role = jwtUtils.getRole(token);

            reservationService.deleteReservationAndUpdateOffer(idReservation, userId, role, token);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Error deleting reservation: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<ReservationDto>> getReservationsByUserId(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ReservationDto> reservations = reservationService.getReservationsByUserId(userId);
        if (reservations == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(reservations);
    }

    @GetMapping
    public ResponseEntity<List<ReservationDto>> getAllReservations(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String role = jwtUtils.getRole(token);
        if (!role.equals(RoleType.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/user/reservations-with-offers")
    public ResponseEntity<List<ReservationDto>> getReservationsWithOffersByUserId(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(reservationService.getReservationsWithOffersByUserId(userId));
    }
}