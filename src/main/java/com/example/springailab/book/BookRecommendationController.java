package com.example.springailab.book;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookRecommendationController {

    private final BookRecommendationService bookRecommendationService;

    @GetMapping("/book/recommendations")
    public List<BookRecommendation> getBookRecommendations(@RequestParam final String genre) {
        return this.bookRecommendationService.getBookRecommendations(genre);
    }
}
