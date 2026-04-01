package com.example.springailab.book;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookRecommendationService {

    private static final ParameterizedTypeReference<List<BookRecommendation>> BOOK_RECOMMENDATIONS_TYPE_REF = new ParameterizedTypeReference<>() {};
    private final ChatClient chatClient;

    public List<BookRecommendation> getBookRecommendations(final String genre) {
        return this.chatClient.prompt()
            .user("Suggest 5 books for the genre: " + genre)
            .call()
            .entity(BOOK_RECOMMENDATIONS_TYPE_REF);
    }
}
