package com.anele.sensitivewords.filter;

import com.anele.sensitivewords.word.SensitiveWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilterService {
    private final SensitiveWordRepository repository;
    private final MessageMasker masker;
    public FilterService(SensitiveWordRepository repository, MessageMasker masker) {
        this.repository = repository; this.masker = masker;
    }
    @Transactional(readOnly = true)
    public String filter(String message) { return masker.mask(message, repository.findAllValues()); }
}
