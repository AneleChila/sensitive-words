package com.anele.sensitivewords.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Message filtering", description = "External message sanitization operations")
public class FilterController {
    private final FilterService service;
    public FilterController(FilterService service) { this.service = service; }
    public record FilterRequest(@NotNull @Size(max = 10000) @Schema(description = "Message content; not stored or logged", example = "You need to create a string") String message) {}
    public record FilterResponse(String message) {}

    @PostMapping("/filter")
    @Operation(summary = "Filter a message", description = "Masks configured terms at whole-word boundaries. Requires messages.filter.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(examples = @ExampleObject(value = "{\"message\":\"You need to create a string\"}"))))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Filtered message",
        content = @Content(examples = @ExampleObject(value = "{\"message\":\"You need to ****** a string\"}"))),
        @ApiResponse(responseCode = "400", description = "Invalid message"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Required scope missing")})
    public FilterResponse filter(@Valid @RequestBody FilterRequest request) {
        return new FilterResponse(service.filter(request.message()));
    }
}
