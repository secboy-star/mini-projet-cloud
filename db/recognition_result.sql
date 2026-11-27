-- Apply after attendance_session.sql.
BEGIN;

ALTER TABLE attendance_photo
    ADD CONSTRAINT attendance_photo_session_object_unique UNIQUE (session_id, swift_object_key);

CREATE TABLE recognition_result (
    event_id uuid PRIMARY KEY,
    correlation_id uuid NOT NULL,
    session_id uuid NOT NULL,
    photo_id uuid NOT NULL,
    model_version text NOT NULL CHECK (model_version ~ '[^[:space:]]'),
    received_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id, photo_id) REFERENCES attendance_photo (session_id, swift_object_key)
);

CREATE TABLE recognition_candidate (
    event_id uuid NOT NULL REFERENCES recognition_result (event_id),
    candidate_position integer NOT NULL CHECK (candidate_position > 0),
    student_id text NOT NULL CHECK (student_id ~ '[^[:space:]]'),
    score double precision NOT NULL CHECK (score BETWEEN 0 AND 1),
    PRIMARY KEY (event_id, candidate_position)
);

COMMIT;
