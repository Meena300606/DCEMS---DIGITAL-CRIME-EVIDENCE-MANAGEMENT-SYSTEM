package com.dcems.db;

import com.dcems.util.Hash;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Inserts default users and a little demo data so the dashboard looks alive on first run. */
final class SeedData {
    private SeedData() {}

    static void seedIfEmpty() {
        if (Database.scalar("SELECT COUNT(*) FROM users") == 0) {
            Database.insert("INSERT INTO users(username,password_hash,full_name,role) VALUES (?,?,?,?)",
                    "admin", Hash.sha256("admin123"), "Chief Inspector Admin", "ADMIN");
            Database.insert("INSERT INTO users(username,password_hash,full_name,role) VALUES (?,?,?,?)",
                    "investigator", Hash.sha256("invest123"), "Riya Sharma", "INVESTIGATOR");
            Database.insert("INSERT INTO users(username,password_hash,full_name,role) VALUES (?,?,?,?)",
                    "analyst", Hash.sha256("analyst123"), "Arjun Mehta", "ANALYST");
        }
        if (Database.scalar("SELECT COUNT(*) FROM crime_cases") > 0) return;

        int u1 = (int) Database.scalar("SELECT id FROM users WHERE username='investigator'");
        int u2 = (int) Database.scalar("SELECT id FROM users WHERE username='analyst'");
        LocalDate today = LocalDate.now();

        int c1 = caze("CASE-" + today.getYear() + "-001", "Online Banking Phishing Ring",
                "Customers received fake bank SMS links; credentials were harvested.", "Cyber Fraud", "INVESTIGATING", "HIGH", u1, today.minusDays(25));
        int c2 = caze("CASE-" + today.getYear() + "-002", "Corporate Data Breach – Retail Chain",
                "Unauthorised access to customer database via compromised VPN account.", "Data Breach", "OPEN", "CRITICAL", u2, today.minusDays(12));
        int c3 = caze("CASE-" + today.getYear() + "-003", "Social Media Identity Theft",
                "Fake profiles created using stolen photos to scam victims.", "Identity Theft", "INVESTIGATING", "MEDIUM", u1, today.minusDays(40));
        caze("CASE-" + today.getYear() + "-004", "Ransomware on Hospital Network",
                "Encrypted patient records; ransom demanded in cryptocurrency.", "Ransomware", "CLOSED", "HIGH", u2, today.minusDays(90));

        Database.insert("INSERT INTO suspects(full_name,alias,age,phone,address,notes,case_id) VALUES (?,?,?,?,?,?,?)",
                "Vikram Rao", "V_Rao", 29, "+91 90000 11111", "Hyderabad", "Owns the phishing domain registrar account.", c1);
        Database.insert("INSERT INTO suspects(full_name,alias,age,phone,address,notes,case_id) VALUES (?,?,?,?,?,?,?)",
                "Unknown (IP 103.21.x.x)", "ghost_admin", null, "", "Unknown", "VPN login source traced to a proxy chain.", c2);
        Database.insert("INSERT INTO suspects(full_name,alias,age,phone,address,notes,case_id) VALUES (?,?,?,?,?,?,?)",
                "Sana Iqbal", "sana.styles", 24, "+91 98888 22222", "Pune", "Admin of the fake profiles.", c3);

        ev(c1, "EV-" + today.getYear() + "-0001", "Mobile Phone", "Seized Android phone with phishing SMS kit installed", "Samsung Galaxy A52", "Suspect residence", "IN_ANALYSIS", u1, 20);
        ev(c1, "EV-" + today.getYear() + "-0002", "Network Logs", "Web server access logs of the cloned banking portal", "Linux web server", "Hosting provider", "VERIFIED", u2, 18);
        ev(c2, "EV-" + today.getYear() + "-0003", "Storage Device", "Forensic image of compromised database server disk", "Dell PowerEdge R740", "Data centre rack 12", "COLLECTED", u2, 10);
        ev(c2, "EV-" + today.getYear() + "-0004", "Email", "Spear-phishing email that delivered the VPN credential stealer", "Outlook mailbox export", "Mail gateway", "IN_ANALYSIS", u1, 9);
        ev(c3, "EV-" + today.getYear() + "-0005", "Screenshot", "Screenshots of fake profiles and scam chats", "Victim smartphone", "Victim statement", "VERIFIED", u1, 30);
        ev(c3, "EV-" + today.getYear() + "-0006", "Chat/Messages", "Exported WhatsApp chat between scammer and victim", "iPhone 13", "Victim statement", "ARCHIVED", u1, 28);

        Database.insert("INSERT INTO activity_log(username,action,detail,created_at) VALUES (?,?,?,?)",
                "System", "SETUP", "Database initialised with demo data", LocalDateTime.now());
    }

    private static int caze(String no, String title, String desc, String cat, String status, String pr, int user, LocalDate opened) {
        return Database.insert("INSERT INTO crime_cases(case_no,title,description,category,status,priority,assigned_to,opened_on) VALUES (?,?,?,?,?,?,?,?)",
                no, title, desc, cat, status, pr, user, opened);
    }

    private static void ev(int caseId, String no, String type, String desc, String src, String loc, String status, int user, int daysAgo) {
        LocalDateTime when = LocalDateTime.now().minusDays(daysAgo).withNano(0);
        int id = Database.insert("INSERT INTO evidence(evidence_no,case_id,type,description,source_device,location_found,collected_by,collected_at,status) VALUES (?,?,?,?,?,?,?,?,?)",
                no, caseId, type, desc, src, loc, user, when, status);
        String who = Database.query("SELECT full_name FROM users WHERE id=?", r -> r.getString(1), user).get(0);
        Database.insert("INSERT INTO custody_log(evidence_id,action,from_person,to_person,remarks,logged_by,logged_at) VALUES (?,?,?,?,?,?,?)",
                id, "COLLECTED", "Scene", who, "Initial evidence capture", who, when);
        if (!status.equals("COLLECTED")) {
            Database.insert("INSERT INTO custody_log(evidence_id,action,from_person,to_person,remarks,logged_by,logged_at) VALUES (?,?,?,?,?,?,?)",
                    id, "SENT_TO_LAB", who, "Forensic Lab", "Sent for analysis", who, when.plusHours(5));
        }
    }
}
