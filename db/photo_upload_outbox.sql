-- Apply after attendance_session.sql. Only new, session-linked uploads enqueue an event.
BEGIN;

CREATE TABLE photo_upload_outbox (
    event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    swift_object_key uuid NOT NULL UNIQUE REFERENCES attendance_photo (swift_object_key),
    session_id uuid NOT NULL REFERENCES attendance_session (session_id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at timestamptz
);

CREATE FUNCTION enqueue_photo_upload() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO photo_upload_outbox (swift_object_key, session_id)
    VALUES (NEW.swift_object_key, NEW.session_id);
    RETURN NEW;
END;
$$;

CREATE TRIGGER photo_upload_after_insert
AFTER INSERT ON attendance_photo
FOR EACH ROW WHEN (NEW.session_id IS NOT NULL)
EXECUTE FUNCTION enqueue_photo_upload();

COMMIT;
