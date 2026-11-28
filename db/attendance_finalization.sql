-- Apply after recognition_result.sql. Write the finalization and its entries in one transaction.
BEGIN;

ALTER TABLE recognition_result
    ADD CONSTRAINT recognition_result_session_event_unique UNIQUE (session_id, event_id);

CREATE TABLE attendance_finalization (
    session_id uuid PRIMARY KEY,
    result_event_id uuid NOT NULL,
    minimum_score double precision NOT NULL CHECK (minimum_score BETWEEN 0 AND 1),
    finalized_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id, result_event_id) REFERENCES recognition_result (session_id, event_id)
);

CREATE TABLE attendance_finalized_entry (
    session_id uuid NOT NULL REFERENCES attendance_finalization (session_id),
    student_id text NOT NULL CHECK (student_id ~ '[^[:space:]]'),
    list_position integer NOT NULL CHECK (list_position > 0),
    status text NOT NULL CHECK (status IN ('P', 'A')),
    PRIMARY KEY (session_id, student_id),
    UNIQUE (session_id, list_position)
);

COMMIT;
