package com.spendsense.api.service.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Account;
import com.spendsense.api.domain.finance.AccountType;
import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.domain.finance.ImportFailure;
import com.spendsense.api.domain.finance.ImportFailureSeverity;
import com.spendsense.api.domain.finance.ImportJob;
import com.spendsense.api.domain.finance.ImportMapping;
import com.spendsense.api.domain.finance.IngestionSession;
import com.spendsense.api.domain.finance.IngestionSource;
import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.TransactionStatus;
import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.dto.finance.CsvColumnMappingRequest;
import com.spendsense.api.dto.finance.CsvImportSummaryResponse;
import com.spendsense.api.dto.finance.CsvPreviewResponse;
import com.spendsense.api.dto.finance.CsvPreviewRowResponse;
import com.spendsense.api.dto.finance.ImportFailureResponse;
import com.spendsense.api.dto.finance.ImportJobResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.ImportMapper;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.ImportFailureRepository;
import com.spendsense.api.repository.finance.ImportJobRepository;
import com.spendsense.api.repository.finance.ImportMappingRepository;
import com.spendsense.api.repository.finance.IngestionSessionRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.ingestion.CsvParserService.CsvRow;
import com.spendsense.api.service.ingestion.CsvParserService.ParsedCsv;
import com.spendsense.api.service.ingestion.TransactionNormalizationService.NormalizedTransaction;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvImportService {
    private static final int PREVIEW_LIMIT = 10;

    private final UserProfileSyncService userProfileSyncService;
    private final CsvParserService csvParserService;
    private final CsvColumnMappingService csvColumnMappingService;
    private final TransactionNormalizationService normalizationService;
    private final CategoryAutoMappingService categoryAutoMappingService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IngestionSessionRepository ingestionSessionRepository;
    private final ImportJobRepository importJobRepository;
    private final ImportFailureRepository importFailureRepository;
    private final ImportMappingRepository importMappingRepository;
    private final ImportMapper importMapper;
    private final ObjectMapper objectMapper;

    public CsvImportService(
            UserProfileSyncService userProfileSyncService,
            CsvParserService csvParserService,
            CsvColumnMappingService csvColumnMappingService,
            TransactionNormalizationService normalizationService,
            CategoryAutoMappingService categoryAutoMappingService,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            IngestionSessionRepository ingestionSessionRepository,
            ImportJobRepository importJobRepository,
            ImportFailureRepository importFailureRepository,
            ImportMappingRepository importMappingRepository,
            ImportMapper importMapper,
            ObjectMapper objectMapper
    ) {
        this.userProfileSyncService = userProfileSyncService;
        this.csvParserService = csvParserService;
        this.csvColumnMappingService = csvColumnMappingService;
        this.normalizationService = normalizationService;
        this.categoryAutoMappingService = categoryAutoMappingService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ingestionSessionRepository = ingestionSessionRepository;
        this.importJobRepository = importJobRepository;
        this.importFailureRepository = importFailureRepository;
        this.importMappingRepository = importMappingRepository;
        this.importMapper = importMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CsvPreviewResponse preview(
            SupabasePrincipal principal,
            MultipartFile file,
            CsvColumnMappingRequest requestedMapping
    ) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        ParsedCsv parsed = csvParserService.parse(file);
        CsvColumnMappingRequest mapping = csvColumnMappingService.mergeWithDetected(parsed.headers(), requestedMapping);
        validateMapping(mapping);

        List<CsvPreviewRowResponse> previewRows = new ArrayList<>();
        List<ImportFailureResponse> failures = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int validRows = 0;
        int duplicateRows = 0;
        UUID previewAccountId = UUID.nameUUIDFromBytes(("preview:" + parsed.checksum()).getBytes());

        for (CsvRow row : parsed.rows()) {
            try {
                NormalizedTransaction normalized = normalizationService.normalize(row, mapping, profile.getId(), previewAccountId);
                boolean duplicate = !seenInFile.add(normalized.dedupeFingerprint())
                        || transactionRepository.existsByUserProfileIdAndDedupeFingerprint(
                        profile.getId(),
                        normalized.dedupeFingerprint()
                );
                if (duplicate) {
                    duplicateRows++;
                } else {
                    validRows++;
                }
                if (previewRows.size() < PREVIEW_LIMIT) {
                    previewRows.add(toPreviewRow(row, normalized, duplicate));
                }
            } catch (IllegalArgumentException exception) {
                failures.add(new ImportFailureResponse(
                        null,
                        row.rowNumber(),
                        "ROW_VALIDATION",
                        exception.getMessage(),
                        ImportFailureSeverity.ERROR,
                        writeJson(row.raw())
                ));
            }
        }

        return new CsvPreviewResponse(
                parsed.filename(),
                parsed.checksum(),
                parsed.signature(),
                parsed.headers(),
                mapping,
                parsed.rows().size(),
                validRows,
                failures.size(),
                duplicateRows,
                previewRows,
                failures.stream().limit(25).toList()
        );
    }

    @Transactional
    public CsvImportSummaryResponse importCsv(
            SupabasePrincipal principal,
            MultipartFile file,
            CsvColumnMappingRequest requestedMapping,
            UUID accountId,
            String accountName,
            String institutionName,
            String idempotencyKey
    ) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = importJobRepository.findByUserProfileIdAndIdempotencyKey(profile.getId(), idempotencyKey.trim());
            if (existing.isPresent()) {
                ImportJob job = existing.get();
                List<ImportFailureResponse> failures = importFailureRepository.findByImportJobIdOrderByRowNumberAsc(job.getId())
                        .stream()
                        .map(importMapper::toResponse)
                        .toList();
                return new CsvImportSummaryResponse(
                        importMapper.toResponse(job),
                        job.getRecordsSeen(),
                        job.getRecordsImported(),
                        job.getRecordsDuplicate(),
                        job.getRecordsFailed(),
                        failures
                );
            }
        }

        ParsedCsv parsed = csvParserService.parse(file);
        CsvColumnMappingRequest mapping = csvColumnMappingService.mergeWithDetected(parsed.headers(), requestedMapping);
        validateMapping(mapping);
        Account account = resolveAccount(profile, accountId, accountName, institutionName, parsed.checksum());

        IngestionSession session = ingestionSessionRepository.save(new IngestionSession(
                profile,
                IngestionSource.CSV,
                writeJson(new ImportMetadata(parsed.filename(), parsed.checksum(), parsed.signature()))
        ));
        ImportJob job = importJobRepository.save(new ImportJob(
                profile,
                session,
                account,
                parsed.filename(),
                parsed.checksum(),
                blankToNull(idempotencyKey),
                writeJson(mapping)
        ));

        int imported = 0;
        int duplicates = 0;
        int failed = 0;
        BigDecimal balanceDelta = BigDecimal.ZERO;
        Set<String> seenInFile = new HashSet<>();
        List<ImportFailure> failures = new ArrayList<>();

        for (CsvRow row : parsed.rows()) {
            try {
                NormalizedTransaction normalized = normalizationService.normalize(row, mapping, profile.getId(), account.getId());
                String rowIdempotencyKey = rowIdempotencyKey(parsed.checksum(), row.rowNumber(), normalized.dedupeFingerprint());
                boolean duplicate = !seenInFile.add(normalized.dedupeFingerprint())
                        || transactionRepository.existsByUserProfileIdAndDedupeFingerprint(profile.getId(), normalized.dedupeFingerprint())
                        || transactionRepository.existsByUserProfileIdAndIdempotencyKey(profile.getId(), rowIdempotencyKey);
                if (duplicate) {
                    duplicates++;
                    failures.add(failure(job, row, "DUPLICATE_TRANSACTION", "This transaction already appears to be imported.", ImportFailureSeverity.WARNING));
                    continue;
                }
                Category category = categoryAutoMappingService.map(normalized.merchantNormalized(), normalized.description());
                transactionRepository.save(toTransaction(profile, account, session, normalized, category, row, rowIdempotencyKey));
                balanceDelta = balanceDelta.add(balanceDelta(normalized.amount(), normalized.direction()));
                imported++;
            } catch (IllegalArgumentException exception) {
                failed++;
                failures.add(failure(job, row, "ROW_VALIDATION", exception.getMessage(), ImportFailureSeverity.ERROR));
            }
        }

        if (!failures.isEmpty()) {
            importFailureRepository.saveAll(failures);
        }
        if (balanceDelta.signum() != 0) {
            account.applyBalanceDelta(balanceDelta);
            accountRepository.save(account);
        }
        session.complete(parsed.rows().size(), imported, duplicates);
        ingestionSessionRepository.save(session);
        job.complete(
                parsed.rows().size(),
                imported,
                duplicates,
                failed,
                writeJson(new ImportSummary(imported, duplicates, failed))
        );
        importJobRepository.save(job);
        saveMapping(profile, parsed.signature(), mapping);

        List<ImportFailureResponse> failureResponses = failures.stream()
                .map(importMapper::toResponse)
                .limit(50)
                .toList();
        return new CsvImportSummaryResponse(
                importMapper.toResponse(job),
                parsed.rows().size(),
                imported,
                duplicates,
                failed,
                failureResponses
        );
    }

    @Transactional
    public List<ImportJobResponse> history(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return importJobRepository.findTop20ByUserProfileIdOrderByStartedAtDesc(userProfileId)
                .stream()
                .map(importMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<ImportFailureResponse> failures(SupabasePrincipal principal, UUID jobId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        ImportJob job = importJobRepository.findByIdAndUserProfileId(jobId, userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found."));
        return importFailureRepository.findByImportJobIdOrderByRowNumberAsc(job.getId())
                .stream()
                .map(importMapper::toResponse)
                .toList();
    }

    public CsvColumnMappingRequest parseMapping(String mappingJson) {
        if (mappingJson == null || mappingJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(mappingJson, CsvColumnMappingRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Column mapping could not be understood.", exception);
        }
    }

    private Account resolveAccount(
            UserProfile profile,
            UUID accountId,
            String accountName,
            String institutionName,
            String fileChecksum
    ) {
        if (accountId != null) {
            return accountRepository.findByIdAndUserProfileId(accountId, profile.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        }
        String name = accountName == null || accountName.isBlank() ? "CSV Imported Account" : accountName.trim();
        String institution = institutionName == null || institutionName.isBlank() ? "CSV Import" : institutionName.trim();
        String sourceAccountId = "csv-" + fileChecksum.substring(0, 24);
        var existingCsvAccount = accountRepository.findByUserProfileIdAndSourceAndSourceAccountId(
                profile.getId(),
                IngestionSource.CSV,
                sourceAccountId
        );
        if (existingCsvAccount.isPresent()) {
            return existingCsvAccount.get();
        }
        return accountRepository.save(new Account(
                profile,
                name,
                institution,
                AccountType.SAVINGS,
                null,
                "INR",
                BigDecimal.ZERO,
                IngestionSource.CSV,
                sourceAccountId
        ));
    }

    private Transaction toTransaction(
            UserProfile profile,
            Account account,
            IngestionSession session,
            NormalizedTransaction normalized,
            Category category,
            CsvRow row,
            String idempotencyKey
    ) {
        return new Transaction(
                profile,
                account,
                category,
                session,
                normalized.amount(),
                normalized.currency(),
                normalized.direction(),
                TransactionStatus.POSTED,
                normalized.occurredAt(),
                normalized.occurredAt(),
                normalized.merchantName(),
                normalized.merchantNormalized(),
                normalized.description(),
                normalized.reference(),
                IngestionSource.CSV,
                "csv-row-" + row.rowNumber(),
                idempotencyKey,
                normalized.dedupeFingerprint(),
                writeJson(row.raw())
        );
    }

    private CsvPreviewRowResponse toPreviewRow(CsvRow row, NormalizedTransaction normalized, boolean duplicate) {
        return new CsvPreviewRowResponse(
                row.rowNumber(),
                row.raw(),
                normalized.occurredAt(),
                normalized.amount(),
                normalized.direction(),
                normalized.merchantName(),
                normalized.description(),
                normalized.reference(),
                duplicate,
                duplicate ? "Possible duplicate" : null
        );
    }

    private ImportFailure failure(
            ImportJob job,
            CsvRow row,
            String code,
            String message,
            ImportFailureSeverity severity
    ) {
        return new ImportFailure(job, row.rowNumber(), code, message, severity, writeJson(row.raw()));
    }

    private void validateMapping(CsvColumnMappingRequest mapping) {
        if (mapping == null || mapping.date() == null || mapping.date().isBlank()) {
            throw new IllegalArgumentException("Map the transaction date column before importing.");
        }
        boolean hasAmount = mapping.amount() != null && !mapping.amount().isBlank();
        boolean hasSplitAmount = mapping.debitAmount() != null && !mapping.debitAmount().isBlank()
                || mapping.creditAmount() != null && !mapping.creditAmount().isBlank();
        if (!hasAmount && !hasSplitAmount) {
            throw new IllegalArgumentException("Map an amount column, or debit and credit amount columns.");
        }
    }

    private void saveMapping(UserProfile profile, String fileSignature, CsvColumnMappingRequest mapping) {
        String mappingJson = writeJson(mapping);
        ImportMapping importMapping = importMappingRepository
                .findByUserProfileIdAndSourceAndFileSignature(profile.getId(), IngestionSource.CSV, fileSignature)
                .orElseGet(() -> new ImportMapping(profile, "CSV mapping", fileSignature, mappingJson));
        importMapping.refresh(mappingJson);
        importMappingRepository.save(importMapping);
    }

    private BigDecimal balanceDelta(BigDecimal amount, TransactionDirection direction) {
        return direction == TransactionDirection.CREDIT ? amount : amount.negate();
    }

    private String rowIdempotencyKey(String fileChecksum, int rowNumber, String dedupeFingerprint) {
        return "csv:" + fileChecksum.substring(0, 24) + ":" + rowNumber + ":" + dedupeFingerprint.substring(0, 24);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize import metadata.", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ImportMetadata(String filename, String checksum, String signature) {
    }

    private record ImportSummary(int imported, int duplicates, int failed) {
    }
}
