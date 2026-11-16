package com.dcems.model;

import java.time.LocalDateTime;

public class Evidence {
    public int id, caseId;
    public long fileSize;
    public String evidenceNo, caseNo, type, description, sourceDevice, location;
    public String fileName, filePath, sha256, collector, status;
    public int collectedBy;
    public LocalDateTime collectedAt;

    @Override public String toString() {
        if (description == null) return evidenceNo;
        String d = description.length() > 40 ? description.substring(0, 40) + "…" : description;
        return evidenceNo + " – " + d;
    }
}
