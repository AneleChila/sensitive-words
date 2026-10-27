package com.anele.sensitivewords.word;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class WordService {
    private final SensitiveWordRepository repository;
    private final TermNormalizer normalizer;
    public WordService(SensitiveWordRepository repository, TermNormalizer normalizer) {
        this.repository = repository; this.normalizer = normalizer;
    }

    public Page<SensitiveWord> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("value")));
    }
    public SensitiveWord get(UUID id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Sensitive word not found"));
    }
    @Transactional
    public SensitiveWord create(String input) {
        String value = normalizer.normalize(input);
        if (repository.existsByValue(value)) throw duplicate();
        return repository.saveAndFlush(new SensitiveWord(value));
    }
    @Transactional
    public SensitiveWord update(UUID id, String input) {
        SensitiveWord word = get(id);
        String value = normalizer.normalize(input);
        if (repository.existsByValueAndIdNot(value, id)) throw duplicate();
        word.setValue(value);
        return repository.saveAndFlush(word);
    }
    @Transactional
    public void delete(UUID id) {
        repository.delete(get(id));
        repository.flush();
    }
    private static ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Sensitive word already exists");
    }
}
