package com.example.springailab.book;

import com.example.springailab.book.dto.BookRecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class BookRecommendationService {

    private static final ParameterizedTypeReference<BookRecommendationResponse> BOOK_RECOMMENDATIONS_TYPE_REF = new ParameterizedTypeReference<>() {};
    private final ChatClient chatClient;

    public BookRecommendationResponse getBookRecommendations(final String genre) {
        return this.chatClient.prompt()
            .user("Suggest 5 books for the genre: " + genre)
            .call()
            .entity(BOOK_RECOMMENDATIONS_TYPE_REF);
    }

    public Flux<String> streamBooks(final String topic) {
        return this.chatClient.prompt()
            .user("List 10 books about " + topic + " in JSON") // Prompt for JSON but stream the RAW text
            .stream()
            .content();
    }
}
