ALTER TABLE "vaccine_record"
    ADD COLUMN IF NOT EXISTS "disease_id" BIGINT,
    ADD COLUMN IF NOT EXISTS "dose_order" INTEGER;

ALTER TABLE "vaccine_record"
    ADD CONSTRAINT "fk_vaccine_record_child_disease"
    FOREIGN KEY ("disease_id") REFERENCES "child_vaccine_diseases" ("id");

CREATE INDEX IF NOT EXISTS "idx_vaccine_record_profile_disease_dose"
    ON "vaccine_record" ("profile_id", "disease_id", "dose_order");
