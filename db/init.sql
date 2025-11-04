-- Create table for tickets
CREATE TABLE IF NOT EXISTS tickets
    (
        id SERIAL PRIMARY KEY,
        user_id INT NOT NULL,
        event_id INT NOT NULL
    );