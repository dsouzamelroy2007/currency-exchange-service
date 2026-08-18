CREATE TABLE currency_pair (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency_id INT,
    to_currency_id INT,
    CONSTRAINT fk_currency_pair_from FOREIGN KEY (from_currency_id) REFERENCES currency(id),
    CONSTRAINT fk_currency_pair_to FOREIGN KEY (to_currency_id) REFERENCES currency(id),
    CONSTRAINT uq_currency_pair UNIQUE (from_currency_id, to_currency_id)
);

ALTER TABLE historical_exchange_rate ADD COLUMN currency_pair_id BIGINT;
ALTER TABLE historical_exchange_rate ADD CONSTRAINT fk_historical_exchange_rate_currency_pair FOREIGN KEY (currency_pair_id) REFERENCES currency_pair(id);
ALTER TABLE historical_exchange_rate DROP CONSTRAINT fk_historical_exchange_rate_currency;
ALTER TABLE historical_exchange_rate DROP COLUMN currency_id;
ALTER TABLE historical_exchange_rate ADD CONSTRAINT uq_historical_exchange_rate_pair_date UNIQUE (currency_pair_id, date);
