CREATE TABLE IF NOT EXISTS CATEGORY (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS MESSAGE (
    id            UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    body          CLOB,
    category_code VARCHAR(100) NOT NULL,
    CONSTRAINT fk_message_category FOREIGN KEY (category_code) REFERENCES CATEGORY(code)
);
