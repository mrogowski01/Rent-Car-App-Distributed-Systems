-- Tworzenie tabeli Roles
CREATE TABLE public.roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Tworzenie tabeli Users
CREATE TABLE public.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE SET NULL
);

-- Tworzenie tabeli Car
CREATE TABLE public.car (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    prod_year INT NOT NULL,
    engine DOUBLE PRECISION NOT NULL,
    fuel_type VARCHAR(50) NOT NULL,
    color VARCHAR(50) NOT NULL,
    gear_type VARCHAR(50) NOT NULL,
    id_user BIGINT,
    FOREIGN KEY (id_user) REFERENCES public.users(id) ON DELETE SET NULL
);

-- Tworzenie tabeli Offer
CREATE TABLE public.offer (
    id_offer BIGSERIAL PRIMARY KEY,
    id_car BIGINT NOT NULL,
    id_user BIGINT,
    price BIGINT NOT NULL,
    available_from DATE NOT NULL,
    available_to DATE NOT NULL,
    FOREIGN KEY (id_car) REFERENCES public.car(id) ON DELETE CASCADE,
    FOREIGN KEY (id_user) REFERENCES public.users(id) ON DELETE SET NULL
);

-- Tworzenie tabeli Reservation
CREATE TABLE public.reservation (
    id_reservation BIGSERIAL PRIMARY KEY,
    id_offer BIGINT NOT NULL,
    id_user BIGINT,
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    FOREIGN KEY (id_offer) REFERENCES public.offer(id_offer) ON DELETE CASCADE,
    FOREIGN KEY (id_user) REFERENCES public.users(id) ON DELETE SET NULL
);

-- Dodanie ról (ROLE_ADMIN, ROLE_USER)
INSERT INTO public.roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO public.roles (name) VALUES ('ROLE_USER');
