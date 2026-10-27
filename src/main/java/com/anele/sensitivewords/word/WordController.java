package com.anele.sensitivewords.word;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/internal/v1/sensitive-words")
@Validated
public class WordController {
    private final WordService service;
    private final ImportService importer;
    public WordController(WordService service, ImportService importer) { this.service = service; this.importer = importer; }
    public record WordRequest(@NotBlank @Size(max = 255) String value) {}
    public record WordResponse(UUID id, String value) {
        static WordResponse from(SensitiveWord word) { return new WordResponse(word.getId(), word.getValue()); }
    }
    public record WordPage(List<WordResponse> content, int page, int size, long totalElements, int totalPages) {}

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ImportService.ImportResult importWords(@RequestPart("file") MultipartFile file) {
        return importer.importFile(file);
    }

    @GetMapping
    public WordPage list(@RequestParam(defaultValue = "0") @Min(0) int page,
                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = service.list(page, size);
        return new WordPage(result.map(WordResponse::from).getContent(), page, size,
                            result.getTotalElements(), result.getTotalPages());
    }
    @GetMapping("/{id}")
    public WordResponse get(@PathVariable UUID id) { return WordResponse.from(service.get(id)); }
    @PostMapping
    public ResponseEntity<WordResponse> create(@Valid @RequestBody WordRequest request) {
        var result = WordResponse.from(service.create(request.value()));
        return ResponseEntity.created(URI.create("/api/internal/v1/sensitive-words/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    public WordResponse update(@PathVariable UUID id, @Valid @RequestBody WordRequest request) {
        return WordResponse.from(service.update(id, request.value()));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
