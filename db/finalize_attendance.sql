-- Apply after attendance_finalization.sql and recognition_roster_score.sql.
CREATE FUNCTION finalize_attendance(source_event uuid, score_threshold double precision)
RETURNS text LANGUAGE sql AS $$
    WITH source AS (
        SELECT event_id, session_id FROM recognition_result WHERE event_id = source_event
    ), finalized AS (
        INSERT INTO attendance_finalization (session_id, result_event_id, minimum_score)
        SELECT session_id, event_id, score_threshold FROM source
        ON CONFLICT (session_id) DO NOTHING
        RETURNING session_id, result_event_id
    ), entries AS (
        INSERT INTO attendance_finalized_entry (session_id, student_id, list_position, status)
        SELECT finalized.session_id, roster.student_id, roster.list_position,
               CASE WHEN roster.best_score >= score_threshold THEN 'P' ELSE 'A' END
        FROM finalized JOIN recognition_roster_score AS roster ON roster.event_id = finalized.result_event_id
    )
    SELECT CASE
        WHEN EXISTS (SELECT 1 FROM finalized) THEN 'created'
        WHEN EXISTS (SELECT 1 FROM source) THEN 'already_finalized'
        ELSE 'result_not_found'
    END;
$$;
