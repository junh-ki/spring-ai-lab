package com.example.springailab.actor;

import com.example.springailab.actor.dto.ActorFilm;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActorService {

    private final ChatClient chatClient;

    public ActorFilm getActorFilm(final String actor) {
        return this.chatClient.prompt()
            .user(user ->
                user
                    .text("Generate the filmography for {actor}")
                    .param("actor", actor)
            )
            .call()
            .entity(ActorFilm.class); // This automatically creates BeanOutputConverter and appends the format instructions to the prompt and parses the response into the object.
    }
}
