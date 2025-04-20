package pl.edu.agh.car_service.Controllers;

import org.springframework.http.HttpStatus;
import pl.edu.agh.car_service.Configuration.Authorization.JwtUtils;
import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Enums.RoleType;
import pl.edu.agh.car_service.Models.Offer.AddOfferDto;
import pl.edu.agh.car_service.Models.Offer.OfferDto;
import pl.edu.agh.car_service.Models.Response.ResponseMessage;
import pl.edu.agh.car_service.Services.OfferService;
import pl.edu.agh.car_service.Services.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/offers")
public class OfferController {
    private final OfferService offerService;
    private final ReservationService reservationService;
    private final JwtUtils jwtUtils;

    @Autowired
    public OfferController(OfferService offerService, ReservationService reservationService, JwtUtils jwtUtils) {
        this.offerService = offerService;
        this.reservationService = reservationService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping
    public ResponseEntity<?> createOffer(@RequestHeader("Authorization") String token, @RequestBody AddOfferDto offer) {
        try {
            Long userId = jwtUtils.getUserId(token);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Long offerId = offerService.createOffer(offer, userId);
            return ResponseEntity.ok().body(offerId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferDto> getOfferById(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long userId = jwtUtils.getUserId(token);
        String role = jwtUtils.getRole(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var offer = offerService.findByIdOffer(id);
        if (offer == null)
            return ResponseEntity.notFound().build();

        if (!role.equals(RoleType.ROLE_ADMIN.toString()) && !offer.getUserId().equals(userId))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(offer);
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<List<OfferDto>> getOffersByCarId(@RequestHeader("Authorization") String token, @PathVariable Long carId) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<OfferDto> offers = offerService.getOffersByCar(carId);
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/user/offers-with-cars")
    public ResponseEntity<List<OfferDto>> getOffersWithCarsByUserId(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(offerService.getOffersWithCarsByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<OfferDto>> getAllOffers(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String role = jwtUtils.getRole(token);
        if (!role.equals(RoleType.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var offers = offerService.getAllOffersWithCars();
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/user")
    public ResponseEntity<List<OfferDto>> getOffersByUserId(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(offerService.getOffersByUserId(userId));
    }

    // @TODO delete that endpoint ???
//    @GetMapping("/user/cars")
//    public ResponseEntity<List<CarDto>> getCarsForOffersByUserId(@RequestHeader("Authorization") String token) {
//        Long userId = jwtUtils.getUserId(token);
//        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//
//        return ResponseEntity.ok(offerService.getCarsForOffersByUserId(userId));
//    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> updateOffer(@RequestHeader("Authorization") String token, @PathVariable Long id, @RequestBody OfferDto offerDetails) {
        try {
            Long userId = jwtUtils.getUserId(token);
            if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            String role = jwtUtils.getRole(token);

            offerService.updateOffer(id, offerDetails, userId, role);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, e.getMessage()));
        }
    }

    @GetMapping("/available")
    public List<OfferDto> getAvailableOffers() {
        return offerService.getAdjustedOffers();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> deleteOffer(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String role = jwtUtils.getRole(token);
        if (!role.equals(RoleType.ROLE_ADMIN.toString()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            reservationService.deleteReservationsByOfferId(id, userId, role);
            if (offerService.deleteOffer(id, userId, role)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new ResponseMessage(400, "Error while deleting: " + ex.getMessage()));
        }
    }
}