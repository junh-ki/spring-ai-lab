package com.example.springailab.book;

import com.example.springailab.book.dto.BookRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookRecommendationController {

    private final BookRecommendationService bookRecommendationService;

    @GetMapping("/book/recommendations")
    public BookRecommendationResponse getBookRecommendations(@RequestParam final String genre) {
        return this.bookRecommendationService.getBookRecommendations(genre);
    }
}
