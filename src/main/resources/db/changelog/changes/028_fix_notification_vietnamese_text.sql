-- liquibase formatted sql

-- changeset codex:095
-- validCheckSum: ANY
UPDATE "notification"
SET
    "title" = 'Lịch tiêm hôm nay',
    "body" = 'Bé trong hồ sơ ID 2 có lịch tiêm hôm nay: mũi vaccine cần kiểm tra trong sổ tiêm.'
WHERE "type" = 'VACCINE_REMINDER'
  AND "title" = 'Lich tiem hom nay'
  AND "body" = 'Be trong ho so ID 2 co lich tiem hom nay: mui vaccine can kiem tra trong so tiem.';

-- changeset codex:096
-- validCheckSum: ANY
UPDATE "notification"
SET
    "title" = 'Nhắc lịch tiêm',
    "body" = replace(replace(replace(replace("body", 'Be ', 'Bé '), ' co lich tiem hom nay: mui ', ' có lịch tiêm hôm nay: mũi '), ' sap co lich tiem ngay ', ' sắp có lịch tiêm ngày '), ': mui ', ': mũi ')
WHERE "type" = 'VACCINE_REMINDER'
  AND (
      "title" = 'Nhac lich tiem'
      OR "body" LIKE 'Be %'
  );

-- changeset codex:097
-- validCheckSum: ANY
UPDATE "notification"
SET "title" = 'Lịch tiêm hôm nay'
WHERE "type" = 'VACCINE_REMINDER'
  AND "title" = 'Lich tiem hom nay';
