CREATE TABLE public.roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE public.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE SET NULL
);

INSERT INTO public.roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO public.roles (name) VALUES ('ROLE_USER');
