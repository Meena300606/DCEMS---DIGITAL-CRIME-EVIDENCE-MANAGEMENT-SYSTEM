-- Handy queries to demonstrate the database during your viva
USE dcems_db;

-- 1. Evidence with its case and collector (JOIN)
SELECT e.evidence_no, c.case_no, e.type, e.status, u.full_name AS collected_by
FROM evidence e JOIN crime_cases c ON c.id = e.case_id LEFT JOIN users u ON u.id = e.collected_by;

-- 2. Evidence count per case (GROUP BY)
SELECT c.case_no, c.title, COUNT(e.id) AS evidence_items
FROM crime_cases c LEFT JOIN evidence e ON e.case_id = c.id GROUP BY c.id;

-- 3. Full chain of custody of one evidence item
SELECT l.logged_at, l.action, l.from_person, l.to_person, l.remarks
FROM custody_log l JOIN evidence e ON e.id = l.evidence_id
WHERE e.evidence_no = 'EV-2026-0001' ORDER BY l.logged_at;

-- 4. Open cases that have unverified evidence
SELECT DISTINCT c.case_no FROM crime_cases c JOIN evidence e ON e.case_id = c.id
WHERE c.status <> 'CLOSED' AND e.status <> 'VERIFIED';
