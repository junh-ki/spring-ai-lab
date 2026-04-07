package com.example.springailab.search;

import com.example.springailab.etl.service.HybridSearchService;
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
public class HybridSearchController {

    private final HybridSearchService hybridSearchService;

    /**
     * Hybrid search: parallel dense (vector) + lexical (OpenSearch) retrieval with RRF fusion.
     */
    @GetMapping("/api/search/hybrid")
    public List<HybridHit> hybridSearch(@RequestParam("q") final String query) {
        if (StringUtils.isBlank(query)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q is required");
        }
        return this.hybridSearchService.search(query).stream()
            .map(document -> new HybridHit(
                document.getId(),
                document.getText(),
                document.getMetadata()
            ))
            .toList();
    }

    public record HybridHit(String id,
                            String text,
                            Map<String, Object> metadata) {}
}
