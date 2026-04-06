package com.example.springailab.etl.controller;

import com.example.springailab.etl.service.IdempotentIngestionService;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class EtlPipelineController {

    private final IdempotentIngestionService idempotentIngestionService;

    @PostMapping(value = "/etl/import-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> importPdf(@RequestPart("file") final MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file must not be empty");
        }
        final String originalFilename = multipartFile.getOriginalFilename();
        final String safeFilename = StringUtils.isNotBlank(originalFilename)
            ? originalFilename
            : "upload.pdf";
        this.idempotentIngestionService.ingest(getResource(multipartFile, safeFilename));
        return Map.of(
            "status",
            "imported",
            "filename",
            safeFilename
        );
    }

    private Resource getResource(final MultipartFile multipartFile,
                                 final String safeFilename) {
        final Resource pdf;
        try {
            final byte[] bytes = multipartFile.getBytes();
            pdf = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };
        } catch (final IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to read uploaded file",
                exception
            );
        }
        return pdf;
    }
}

