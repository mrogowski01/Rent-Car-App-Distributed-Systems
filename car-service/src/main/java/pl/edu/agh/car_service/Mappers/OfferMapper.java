package pl.edu.agh.car_service.Mappers;

import pl.edu.agh.car_service.Entities.Offer;
import pl.edu.agh.car_service.Models.Offer.AddOfferDto;
import pl.edu.agh.car_service.Models.Offer.OfferDto;

public class OfferMapper {
    public static Offer OfferDtoToOffer(OfferDto offerDto) {
        return new Offer(offerDto.getId(),
                offerDto.getCarId(),
                offerDto.getUserId(),
                offerDto.getPrice(),
                offerDto.getAvailableFrom(),
                offerDto.getAvailableTo(),
                offerDto.getCar() == null ? null : CarMapper.CarDtoToCar(offerDto.getCar()));
    }

    public static OfferDto OfferToOfferDto(Offer offer) {
        return new OfferDto(offer.getIdOffer(),
                offer.getCarId(),
                offer.getIdUser(),
                offer.getPrice(),
                offer.getAvailableFrom(),
                offer.getAvailableTo(),
                offer.getCarDetails() == null ? null : CarMapper.CarToCarDto(offer.getCarDetails()));
    }

    public static Offer AddOfferDtoToOffer(AddOfferDto offerDto, Long userId) {
        return new Offer(null,
                offerDto.getCarId(),
                userId,
                offerDto.getPrice(),
                offerDto.getAvailableFrom(),
                offerDto.getAvailableTo(),
                null);
    }
}