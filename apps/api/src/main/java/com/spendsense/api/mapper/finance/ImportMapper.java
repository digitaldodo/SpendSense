package com.spendsense.api.mapper.finance;

import com.spendsense.api.domain.finance.ImportFailure;
import com.spendsense.api.domain.finance.ImportJob;
import com.spendsense.api.dto.finance.ImportFailureResponse;
import com.spendsense.api.dto.finance.ImportJobResponse;
import org.springframework.stereotype.Component;

@Component
public class ImportMapper {
    private final AccountMapper accountMapper;

    public ImportMapper(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public ImportJobResponse toResponse(ImportJob importJob) {
        return new ImportJobResponse(
                importJob.getId(),
                importJob.getSource(),
                importJob.getStatus(),
                importJob.getOriginalFilename(),
                importJob.getFileChecksum(),
                importJob.getIdempotencyKey(),
                importJob.getRecordsSeen(),
                importJob.getRecordsImported(),
                importJob.getRecordsDuplicate(),
                importJob.getRecordsFailed(),
                importJob.getStartedAt(),
                importJob.getCompletedAt(),
                importJob.getAccount() == null ? null : accountMapper.toResponse(importJob.getAccount())
        );
    }

    public ImportFailureResponse toResponse(ImportFailure failure) {
        return new ImportFailureResponse(
                failure.getId(),
                failure.getRowNumber(),
                failure.getErrorCode(),
                failure.getMessage(),
                failure.getSeverity(),
                failure.getRawRowJson()
        );
    }
}
