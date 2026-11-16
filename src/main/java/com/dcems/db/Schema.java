package com.dcems.db;

/** All CREATE TABLE statements (also available as database/schema.sql). */
final class Schema {
    private Schema() {}

    static final String[] TABLES = {
        """
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(50) NOT NULL UNIQUE,
            password_hash CHAR(64) NOT NULL,
            full_name VARCHAR(100) NOT NULL,
            role ENUM('ADMIN','INVESTIGATOR','ANALYST') NOT NULL DEFAULT 'INVESTIGATOR',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )""",
        """
        CREATE TABLE IF NOT EXISTS crime_cases (
            id INT AUTO_INCREMENT PRIMARY KEY,
            case_no VARCHAR(30) NOT NULL UNIQUE,
            title VARCHAR(150) NOT NULL,
            description TEXT,
            category VARCHAR(50) NOT NULL,
            status ENUM('OPEN','INVESTIGATING','CLOSED') NOT NULL DEFAULT 'OPEN',
            priority ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
            assigned_to INT NULL,
            opened_on DATE NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS suspects (
            id INT AUTO_INCREMENT PRIMARY KEY,
            full_name VARCHAR(100) NOT NULL,
            alias VARCHAR(80),
            age INT,
            phone VARCHAR(30),
            address VARCHAR(200),
            notes TEXT,
            case_id INT NULL,
            FOREIGN KEY (case_id) REFERENCES crime_cases(id) ON DELETE SET NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS evidence (
            id INT AUTO_INCREMENT PRIMARY KEY,
            evidence_no VARCHAR(30) NOT NULL UNIQUE,
            case_id INT NOT NULL,
            type VARCHAR(50) NOT NULL,
            description TEXT NOT NULL,
            source_device VARCHAR(100),
            location_found VARCHAR(150),
            file_name VARCHAR(255),
            file_path VARCHAR(500),
            sha256 CHAR(64),
            file_size BIGINT DEFAULT 0,
            collected_by INT NULL,
            collected_at DATETIME NOT NULL,
            status ENUM('COLLECTED','IN_ANALYSIS','VERIFIED','ARCHIVED') NOT NULL DEFAULT 'COLLECTED',
            FOREIGN KEY (case_id) REFERENCES crime_cases(id) ON DELETE CASCADE,
            FOREIGN KEY (collected_by) REFERENCES users(id) ON DELETE SET NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS custody_log (
            id INT AUTO_INCREMENT PRIMARY KEY,
            evidence_id INT NOT NULL,
            action VARCHAR(40) NOT NULL,
            from_person VARCHAR(100),
            to_person VARCHAR(100),
            remarks VARCHAR(300),
            logged_by VARCHAR(100),
            logged_at DATETIME NOT NULL,
            FOREIGN KEY (evidence_id) REFERENCES evidence(id) ON DELETE CASCADE
        )""",
        """
        CREATE TABLE IF NOT EXISTS activity_log (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100),
            action VARCHAR(50) NOT NULL,
            detail VARCHAR(300),
            created_at DATETIME NOT NULL
        )"""
    };
}
