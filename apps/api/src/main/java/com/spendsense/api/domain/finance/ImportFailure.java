package com.spendsense.api.domain.finance;

import com.spendsense.api.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "import_failures")
public class ImportFailure extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "error_code", nullable = false, length = 80)
    private String errorCode;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private ImportFailureSeverity severity = ImportFailureSeverity.ERROR;

    @Column(name = "raw_row_json", columnDefinition = "text")
    private String rawRowJson;

    protected ImportFailure() {
    }

    public ImportFailure(
            ImportJob importJob,
            int rowNumber,
            String errorCode,
            String message,
            ImportFailureSeverity severity,
            String rawRowJson
    ) {
        setId(UUID.randomUUID());
        this.importJob = importJob;
        this.rowNumber = rowNumber;
        this.errorCode = errorCode;
        this.message = message;
        this.severity = severity;
        this.rawRowJson = rawRowJson;
    }

    public ImportJob getImportJob() {
        return importJob;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public ImportFailureSeverity getSeverity() {
        return severity;
    }

    public String getRawRowJson() {
        return rawRowJson;
    }
}
