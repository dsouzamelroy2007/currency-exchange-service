CREATE TABLE currency (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    abbreviation VARCHAR(255) UNIQUE
);

CREATE TABLE historical_exchange_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate DOUBLE,
    date DATE,
    last_updated TIMESTAMP,
    currency_id INT,
    CONSTRAINT fk_historical_exchange_rate_currency FOREIGN KEY (currency_id) REFERENCES currency(id)
);

CREATE INDEX exchange_rate_history_date ON historical_exchange_rate(date);
