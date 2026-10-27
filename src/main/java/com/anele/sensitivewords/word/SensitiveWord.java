package com.anele.sensitivewords.word;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "sensitive_word")
public class SensitiveWord {
    @Id private UUID id;
    @Column(name = "term_value", nullable = false, unique = true, length = 255) private String value;
    @Version private long version;

    protected SensitiveWord() {}
    public SensitiveWord(String value) { this.id = UUID.randomUUID(); this.value = value; }
    public UUID getId() { return id; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
