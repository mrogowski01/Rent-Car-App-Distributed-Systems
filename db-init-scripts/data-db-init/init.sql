CREATE TABLE public.car (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    prod_year INT NOT NULL,
    engine DOUBLE PRECISION NOT NULL,
    fuel_type VARCHAR(50) NOT NULL,
    color VARCHAR(50) NOT NULL,
    gear_type VARCHAR(50) NOT NULL,
    id_user BIGINT
);

CREATE TABLE public.offer (
    id_offer BIGSERIAL PRIMARY KEY,
    id_car BIGINT NOT NULL,
    id_user BIGINT,
    price BIGINT NOT NULL,
    available_from DATE NOT NULL,
    available_to DATE NOT NULL,
    FOREIGN KEY (id_car) REFERENCES public.car(id) ON DELETE CASCADE
);

CREATE TABLE public.reservation (
    id_reservation BIGSERIAL PRIMARY KEY,
    id_offer BIGINT NOT NULL,
    id_user BIGINT,
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    FOREIGN KEY (id_offer) REFERENCES public.offer(id_offer) ON DELETE CASCADE
);
