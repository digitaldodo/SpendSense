package com.spendsense.api.domain.finance;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_mappings")
public class ImportMapping extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 48)
    private IngestionSource source = IngestionSource.CSV;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "file_signature", nullable = false, length = 160)
    private String fileSignature;

    @Column(name = "mapping_json", nullable = false, columnDefinition = "text")
    private String mappingJson;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt = Instant.now();

    protected ImportMapping() {
    }

    public ImportMapping(UserProfile userProfile, String name, String fileSignature, String mappingJson) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.name = name;
        this.fileSignature = fileSignature;
        this.mappingJson = mappingJson;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public IngestionSource getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    public String getFileSignature() {
        return fileSignature;
    }

    public String getMappingJson() {
        return mappingJson;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void refresh(String mappingJson) {
        this.mappingJson = mappingJson;
        this.lastUsedAt = Instant.now();
    }
}
