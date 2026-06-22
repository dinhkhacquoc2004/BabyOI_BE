-- liquibase formatted sql

-- changeset codex:046-fix-routine-notification-profile-labels
UPDATE notification
SET
    title = replace(
        replace(title, 'của bé Bé ', 'của bé '),
        'của bé bé ', 'của bé '
    ),
    body = replace(
        replace(body, 'Bé Bé ', 'Bé '),
        'Bé bé ', 'Bé '
    )
WHERE type = 'ROUTINE_REMINDER'
  AND (
      title LIKE '%của bé Bé %'
      OR title LIKE '%của bé bé %'
      OR body LIKE 'Bé Bé %'
      OR body LIKE 'Bé bé %'
  );
