-- These are the SQL commands to setup the database, a user, and the tables needed to run this app
CREATE DATABASE image_object_detection;

-- Run the following with your own username and password
CREATE USER your_username WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE image_object_detection TO your_username;
--------------------------------------------------------

CREATE TABLE IF NOT EXISTS image (
	id serial PRIMARY KEY,
	label TEXT NOT NULL,
    image_location TEXT NOT NULL,
	upload_time TIMESTAMP WITHOUT TIME ZONE default (now() at time zone 'utc')
);

CREATE TABLE IF NOT EXISTS image_object_detection (
	id serial PRIMARY KEY,
	image_id INT NOT NULL,
	confidence NUMERIC(16,13) NOT NULL,
	object_detected TEXT NOT NULL,
    CONSTRAINT fk_image_id
      FOREIGN KEY(image_id) 
	    REFERENCES image(id)
);
