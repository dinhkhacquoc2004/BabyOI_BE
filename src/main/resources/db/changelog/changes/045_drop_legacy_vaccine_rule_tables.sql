-- liquibase formatted sql

-- changeset codex:045-drop-legacy-vaccine-rule-tables
DROP TABLE IF EXISTS vaccine_group_dose_schedules CASCADE;
DROP TABLE IF EXISTS vaccine_product_groups CASCADE;
DROP TABLE IF EXISTS vaccine_groups CASCADE;
