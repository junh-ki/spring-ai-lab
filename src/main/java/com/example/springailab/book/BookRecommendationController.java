package com.example.springailab.book;

import com.example.springailab.book.dto.BookRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class BookRecommendationController {

    private final BookRecommendationService bookRecommendationService;

    @GetMapping("/book/recommendations")
    public BookRecommendationResponse getBookRecommendations(@RequestParam final String genre) {
        return this.bookRecommendationService.getBookRecommendations(genre);
    }

    @GetMapping(value = "/book/stream-books", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamBooks(@RequestParam final String topic) {
        return this.bookRecommendationService.streamBooks(topic);
    }
}
