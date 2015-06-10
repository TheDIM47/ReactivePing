CREATE TABLE IF NOT EXISTS Tasks (
  id     IDENTITY     NOT NULL,
  method CHAR(1)      NOT NULL CHECK(method IN ('N','H','F','E')),
  name   VARCHAR(100) UNIQUE NOT NULL,
  url    VARCHAR(250) UNIQUE NOT NULL,
  period INT          NOT NULL DEFAULT 60,
  active BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS PingData (
  id      INT  NOT NULL,
  start   TIMESTAMP NOT NULL,
  rtt     INT,
  status  INT  NOT NULL,
  message VARCHAR(250),
  PRIMARY KEY (id, start),
  FOREIGN KEY (id) REFERENCES Tasks(id)
);
