-- Apply after official_roster.sql and attendance_photo.sql.
BEGIN;

CREATE TABLE attendance_session (
    session_id uuid PRIMARY KEY,
    module_code text NOT NULL,
    roster_version integer NOT NULL,
    FOREIGN KEY (module_code, roster_version) REFERENCES official_roster (module_code, version)
);

-- Existing uploads keep their metadata and can remain without a session.
ALTER TABLE attendance_photo
    ADD COLUMN session_id uuid REFERENCES attendance_session (session_id);

COMMIT;
