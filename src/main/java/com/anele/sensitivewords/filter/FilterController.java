package com.anele.sensitivewords.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
public class FilterController {
    private final FilterService service;
    public FilterController(FilterService service) { this.service = service; }
    public record FilterRequest(@NotNull @Size(max = 10000) String message) {}
    public record FilterResponse(String message) {}

    @PostMapping("/filter")
    public FilterResponse filter(@Valid @RequestBody FilterRequest request) {
        return new FilterResponse(service.filter(request.message()));
    }
}
