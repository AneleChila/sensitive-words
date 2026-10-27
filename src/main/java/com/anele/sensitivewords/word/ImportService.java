package com.anele.sensitivewords.word;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImportService {
    static final long MAX_BYTES = 1024 * 1024;
    static final int MAX_ENTRIES = 10_000;
    private final ObjectMapper mapper;
    private final TermNormalizer normalizer;
    private final SensitiveWordRepository repository;

    public ImportService(ObjectMapper mapper, TermNormalizer normalizer, SensitiveWordRepository repository) {
        this.mapper = mapper; this.normalizer = normalizer; this.repository = repository;
    }

    @Transactional
    public ImportResult importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("Import file must not be empty");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
            "Import file must not exceed 1 MiB");
        List<String> inputs = parse(file);
        if (inputs.size() > MAX_ENTRIES) throw bad("Import file must contain at most 10000 entries");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String input : inputs) unique.add(normalizer.normalize(input));
        Set<String> existing = repository.findValuesIn(unique);
        List<SensitiveWord> additions = unique.stream().filter(value -> !existing.contains(value))
            .map(SensitiveWord::new).toList();
        repository.saveAll(additions);
        repository.flush();
        return new ImportResult(inputs.size(), additions.size(), inputs.size() - additions.size());
    }

    private List<String> parse(MultipartFile file) {
        try {
            JsonNode root = mapper.readTree(file.getBytes());
            if (root == null || !root.isArray()) throw bad("Import content must be a JSON array of strings");
            List<String> values = new ArrayList<>();
            for (JsonNode node : root) {
                if (!node.isTextual()) throw bad("Every import entry must be a string");
                values.add(node.textValue());
            }
            return values;
        } catch (JsonProcessingException exception) {
            throw bad("Import content must be valid UTF-8 JSON");
        } catch (IOException exception) {
            throw bad("Import file could not be read");
        }
    }

    private ResponseStatusException bad(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }

    public record ImportResult(int received, int inserted, int skippedDuplicates) {}
}
