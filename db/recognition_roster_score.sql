-- Apply after recognition_result.sql. A NULL score means no candidate matched this roster entry.
BEGIN;

CREATE VIEW recognition_roster_score AS
SELECT result.event_id, result.session_id, entry.student_id, entry.list_position,
       MAX(candidate.score) AS best_score
FROM recognition_result AS result
JOIN attendance_session AS session ON session.session_id = result.session_id
JOIN official_roster_entry AS entry
    ON entry.module_code = session.module_code AND entry.version = session.roster_version
LEFT JOIN recognition_candidate AS candidate
    ON candidate.event_id = result.event_id AND candidate.student_id = entry.student_id
GROUP BY result.event_id, result.session_id, entry.student_id, entry.list_position;

COMMIT;
