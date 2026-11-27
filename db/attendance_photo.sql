CREATE TABLE attendance_photo (
    swift_object_key uuid PRIMARY KEY,
    content_type text NOT NULL CHECK (content_type = 'image/png'),
    byte_size integer NOT NULL CHECK (byte_size > 0),
    uploaded_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
