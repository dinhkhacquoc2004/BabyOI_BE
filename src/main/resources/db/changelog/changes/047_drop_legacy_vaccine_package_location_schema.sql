--liquibase formatted sql

--changeset codex:047-drop-legacy-vaccine-package-location-schema
ALTER TABLE vaccine_record
    DROP COLUMN IF EXISTS location_id,
    DROP COLUMN IF EXISTS package_id,
    DROP COLUMN IF EXISTS package_structure_id;

DROP TABLE IF EXISTS location_package_prices CASCADE;
DROP TABLE IF EXISTS location_vaccine_prices CASCADE;
DROP TABLE IF EXISTS package_structures CASCADE;
DROP TABLE IF EXISTS vaccine_packages CASCADE;
DROP TABLE IF EXISTS locations CASCADE;
