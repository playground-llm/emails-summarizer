CREATE TABLE IF NOT EXISTS USERS (
    login       VARCHAR(100) NOT NULL PRIMARY KEY,
    github_id   BIGINT,
    name        VARCHAR(255),
    avatar_url  VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS ROLES (
    id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(100) NOT NULL,
    role  VARCHAR(50)  NOT NULL,
    CONSTRAINT fk_roles_user FOREIGN KEY (login) REFERENCES USERS(login),
    CONSTRAINT uq_roles_login_role UNIQUE (login, role)
);

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
