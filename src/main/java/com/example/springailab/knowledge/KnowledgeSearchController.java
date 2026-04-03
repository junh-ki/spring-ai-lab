package com.example.springailab.knowledge;

import com.example.springailab.etl.ParentChildIngestor;
import com.example.springailab.etl.ParentChildRetriever;
import com.example.springailab.search.SearchHelper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * HTTP surface for the RAG-style building blocks: parent/child ingest + retrieval, finance-filtered
 * search, and metadata-filtered search. All paths share the application {@code VectorStore} (lab
 * setup); production would typically isolate corpora or use separate stores.
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeSearchController {

    private final ParentChildIngestor parentChildIngestor;
    private final ParentChildRetriever parentChildRetriever;
    private final SearchHelper searchHelper;

    /**
     * Ingest a document with the parent/child splitter: children (with embeddings) go to the
     * vector store; parent bodies stay in the in-memory parent map for wide-context retrieval.
     */
    @PostMapping(value = "/parent-child/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> ingestParentChild(@RequestPart("file") final MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }
        final String originalFilename = multipartFile.getOriginalFilename();
        final String filename = StringUtils.isNotBlank(originalFilename)
            ? originalFilename
            : "upload.bin";
        this.parentChildIngestor.ingestDocument(toResource(multipartFile, filename));
        return Map.of("status", "ingested", "filename", filename);
    }

    /**
     * Search over child embeddings, then return the corresponding parent chunks (broad context).
     */
    @GetMapping("/parent-child/search")
    public List<DocumentHit> searchParentContext(@RequestParam final String query) {
        if (StringUtils.isBlank(query)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }
        return this.parentChildRetriever.retrieve(query).stream()
            .map(KnowledgeSearchController::toHit)
            .toList();
    }

    /**
     * Similarity search constrained by author and a set of categories (metadata must match at index time).
     */
    @GetMapping("/catalog/search")
    public List<DocumentHit> searchCatalog(@RequestParam final String query,
                                           @RequestParam final String author,
                                           @RequestParam final List<String> categories) {
        if (StringUtils.isBlank(query)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }
        if (StringUtils.isBlank(author)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "author is required");
        }
        if (CollectionUtils.isEmpty(categories)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one category is required");
        }
        return this.searchHelper.dynamicSearch(query, author, categories).stream()
            .map(KnowledgeSearchController::toHit)
            .toList();
    }

    private static DocumentHit toHit(final Document document) {
        return new DocumentHit(
            document.getId(),
            document.getText(),
            document.getMetadata()
        );
    }

    private static Resource toResource(final MultipartFile multipartFile,
                                       final String filename) {
        try {
            final byte[] bytes = multipartFile.getBytes();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (final IOException ioException) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to read uploaded file",
                ioException
            );
        }
    }

    public record DocumentHit(String id,
                              String text,
                              Map<String, Object> metadata) {}
}
