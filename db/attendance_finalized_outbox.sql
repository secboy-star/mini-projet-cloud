-- Apply after attendance_finalization.sql. Only new finalizations enqueue events.
BEGIN;

CREATE TABLE attendance_finalized_outbox (
    event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL UNIQUE REFERENCES attendance_finalization (session_id),
    correlation_id uuid NOT NULL,
    export_version integer NOT NULL DEFAULT 1 CHECK (export_version > 0),
    created_at timestamptz NOT NULL,
    published_at timestamptz
);

CREATE FUNCTION enqueue_attendance_finalized() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO attendance_finalized_outbox (session_id, correlation_id, created_at)
    VALUES (NEW.session_id,
            (SELECT correlation_id FROM recognition_result WHERE event_id = NEW.result_event_id),
            NEW.finalized_at);
    RETURN NEW;
END;
$$;

CREATE TRIGGER attendance_finalization_after_insert
AFTER INSERT ON attendance_finalization
FOR EACH ROW EXECUTE FUNCTION enqueue_attendance_finalized();

COMMIT;
