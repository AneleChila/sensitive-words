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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/internal/v1/sensitive-words")
@Validated
@Tag(name = "Dictionary management", description = "Internal operations requiring words.manage")
public class WordController {
    private final WordService service;
    private final ImportService importer;
    public WordController(WordService service, ImportService importer) { this.service = service; this.importer = importer; }
    public record WordRequest(@NotBlank @Size(max = 255) @Schema(description = "Term or phrase", example = "private phrase") String value) {}
    public record WordResponse(UUID id, String value) {
        static WordResponse from(SensitiveWord word) { return new WordResponse(word.getId(), word.getValue()); }
    }
    public record WordPage(List<WordResponse> content, int page, int size, long totalElements, int totalPages) {}

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "Import dictionary terms", description = "Adds a UTF-8 JSON array of strings atomically. Limit: 1 MiB and 10,000 entries.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Import counts",
        content = @Content(examples = @ExampleObject(value = "{\"received\":3,\"inserted\":2,\"skippedDuplicates\":1}"))),
        @ApiResponse(responseCode = "400", description = "Invalid JSON or term"),
        @ApiResponse(responseCode = "413", description = "File exceeds 1 MiB"),
        @ApiResponse(responseCode = "415", description = "Unsupported media type")})
    public ImportService.ImportResult importWords(@Parameter(description = "UTF-8 JSON array file; maximum 1 MiB")
        @RequestPart("file") MultipartFile file) {
        return importer.importFile(file);
    }

    @GetMapping
    @Operation(summary = "List dictionary terms", description = "Returns normalized terms sorted by value. Page size is 1 to 100.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Page returned"), @ApiResponse(responseCode = "400", description = "Invalid pagination"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Required scope missing")})
    public WordPage list(@RequestParam(defaultValue = "0") @Min(0) int page,
                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = service.list(page, size);
        return new WordPage(result.map(WordResponse::from).getContent(), page, size,
                            result.getTotalElements(), result.getTotalPages());
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get a dictionary term")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Term returned"),
        @ApiResponse(responseCode = "400", description = "Invalid UUID"), @ApiResponse(responseCode = "404", description = "Term not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Required scope missing")})
    public WordResponse get(@Parameter(description = "Term identifier", required = true, example = "00000000-0000-0000-0000-000000000000") @PathVariable UUID id) { return WordResponse.from(service.get(id)); }
    @PostMapping
    @Operation(summary = "Create a dictionary term", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
        content = @Content(examples = @ExampleObject(value = "{\"value\":\"private phrase\"}"))))
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Term created"), @ApiResponse(responseCode = "400", description = "Invalid term"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Required scope missing"), @ApiResponse(responseCode = "409", description = "Normalized duplicate")})
    public ResponseEntity<WordResponse> create(@Valid @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Term or phrase to normalize", required = true,
            content = @Content(examples = @ExampleObject(value = "{\"value\":\"private phrase\"}"))) WordRequest request) {
        var result = WordResponse.from(service.create(request.value()));
        return ResponseEntity.created(URI.create("/api/internal/v1/sensitive-words/" + result.id())).body(result);
    }
    @PutMapping("/{id}")
    @Operation(summary = "Replace a dictionary term", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
        content = @Content(examples = @ExampleObject(value = "{\"value\":\"updated phrase\"}"))))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Term updated"), @ApiResponse(responseCode = "400", description = "Invalid term or UUID"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Required scope missing"), @ApiResponse(responseCode = "404", description = "Term not found"), @ApiResponse(responseCode = "409", description = "Duplicate or concurrent update")})
    public WordResponse update(@Parameter(description = "Term identifier", required = true) @PathVariable UUID id, @Valid @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Replacement term or phrase", required = true,
            content = @Content(examples = @ExampleObject(value = "{\"value\":\"updated phrase\"}"))) WordRequest request) {
        return WordResponse.from(service.update(id, request.value()));
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a dictionary term")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Term deleted"), @ApiResponse(responseCode = "400", description = "Invalid UUID"), @ApiResponse(responseCode = "401", description = "Authentication required"), @ApiResponse(responseCode = "403", description = "Required scope missing"), @ApiResponse(responseCode = "404", description = "Term not found")})
    public ResponseEntity<Void> delete(@Parameter(description = "Term identifier", required = true) @PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
