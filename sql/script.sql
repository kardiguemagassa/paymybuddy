DROP DATABASE IF EXISTS `openclassrooms_paymybuddy`;
CREATE DATABASE IF NOT EXISTS `openclassrooms_paymybuddy` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `openclassrooms_paymybuddy`;

-- Table users
DROP TABLE IF EXISTS `user`;
CREATE TABLE user
(
    `id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
    `profile_name` VARCHAR(100) DEFAULT NULL,
    `email`    VARCHAR(150) DEFAULT NULL UNIQUE,
    `password` VARCHAR(255) DEFAULT NULL,
    `profile_image_url` VARCHAR(255) DEFAULT NULL,
    `balance` DOUBLE DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Table connection
DROP TABLE IF EXISTS `connection`;
CREATE TABLE connection
(
    `user_id`       BIGINT NOT NULL,
    `connection_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `connection_id`),
    UNIQUE (`user_id`, `connection_id`),
    FOREIGN KEY (`user_id`) REFERENCES user (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`connection_id`) REFERENCES user (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Table transactions
DROP TABLE IF EXISTS `transaction`;
CREATE TABLE transaction
(
    `id`            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `sender_id`     BIGINT DEFAULT NULL,
    `receiver_id`   BIGINT DEFAULT NULL,
    `description`   VARCHAR(255) DEFAULT NULL,
    `amount`        DOUBLE DEFAULT NULL,
    `fee`        DOUBLE DEFAULT NULL,
    `execution_date` TIMESTAMP,
    `currency`        VARCHAR(5) DEFAULT NULL,
    FOREIGN KEY (`sender_id`) REFERENCES user (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`receiver_id`) REFERENCES user (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- View historic
CREATE VIEW historic AS
SELECT
    tr.id,
    tr.sender_id,
    sed.profile_name AS sender_profile_name,
    tr.receiver_id,
    rec.profile_name AS receiver_profile_name,
    tr.description,
    tr.amount,
    tr.fee,
    tr.currency,
    tr.execution_date
FROM transaction tr
         INNER JOIN user sed ON tr.sender_id = sed.id
         INNER JOIN user rec ON tr.receiver_id = rec.id;




-- Insertion user
INSERT INTO user (profile_name, email, password, profile_image_url, balance)
VALUES
    ('John', 'john@gmail.com', 'John#123', NULL, 100.00),
    ('Laure', 'laure@gmail.com', 'Laure#123', NULL, 150.00),
    ('Clara', 'clara@gmail.com', 'Clara#123', NULL, 200.00),
    ('Luc', 'luc@gmail.com', 'Luc#123', NULL, 250.00);


-- John est connecté à Laure et Clara
INSERT INTO connection (user_id, connection_id)
VALUES (1, 2), (1, 3);

-- Laure est connecté à Luc
INSERT INTO connection (user_id, connection_id)
VALUES (2, 4);

-- Transactions entre users
INSERT INTO transaction (sender_id, receiver_id, description, amount, fee, execution_date, currency)
VALUES
    (1, 2, 'Remboursement café', 5.00, 0.05, NOW(), 'EUR'),
    (1, 3, 'Participation déjeuner', 12.50, 0.10, NOW(), 'EUR'),
    (2, 4, 'Achat billet concert', 30.00, 0.30, NOW(), 'EUR'),
    (3, 1, 'Partage de covoiturage', 7.75, 0.08, NOW(), 'EUR');

-- tester la vue historic
SELECT * FROM historic;

