CREATE DATABASE IF NOT EXISTS `openclassrooms_paymybuddy`;
USE `openclassrooms_paymybuddy`;

CREATE TABLE users (
    `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE user_connections (
    `user_id` INT NOT NULL,
    `connection_id` INT NOT NULL,
    PRIMARY KEY (`user_id`, `connection_id`),
    FOREIGN KEY (`user_id`) REFERENCES users(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`connection_id`) REFERENCES users(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Table transactions
CREATE TABLE transactions (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` INT NOT NULL,
    `receiver_id` INT NOT NULL,
    `description` VARCHAR(255),
    `amount` DOUBLE NOT NULL,
    FOREIGN KEY (`sender_id`) REFERENCES users(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`receiver_id`) REFERENCES users(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
