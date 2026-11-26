BEGIN;

CREATE TABLE official_roster (
    module_code text NOT NULL CHECK (module_code ~ '[^[:space:]]'),
    version integer NOT NULL CHECK (version > 0),
    PRIMARY KEY (module_code, version)
);

CREATE TABLE official_roster_entry (
    module_code text NOT NULL,
    version integer NOT NULL,
    student_id text NOT NULL CHECK (student_id ~ '[^[:space:]]'),
    list_position integer NOT NULL CHECK (list_position > 0),
    PRIMARY KEY (module_code, version, student_id),
    UNIQUE (module_code, version, list_position),
    FOREIGN KEY (module_code, version) REFERENCES official_roster (module_code, version)
);

COMMIT;
