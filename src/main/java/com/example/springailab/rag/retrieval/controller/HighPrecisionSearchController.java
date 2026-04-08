package com.example.springailab.rag.retrieval.controller;

import com.example.springailab.rag.retrieval.service.HighPrecisionSearchService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class HighPrecisionSearchController {

    private final HighPrecisionSearchService highPrecisionSearchService;

    @GetMapping("/api/search/high-precision")
    public List<HighPrecisionHit> search(@RequestParam("q") final String query) {
        if (StringUtils.isBlank(query)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q is required");
        }
        return this.highPrecisionSearchService.search(query).stream()
            .map(document ->
                new HighPrecisionHit(
                    document.getId(),
                    document.getText(),
                    document.getMetadata()
                )
            )
            .toList();
    }

    public record HighPrecisionHit(String id,
                                   String text,
                                   Map<String, Object> metadata) {}
}
