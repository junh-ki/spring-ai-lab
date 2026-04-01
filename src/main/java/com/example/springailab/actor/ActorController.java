package com.example.springailab.actor;

import com.example.springailab.actor.dto.ActorFilm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActorController {

    private final ActorService actorService;

    @GetMapping("/films")
    public ActorFilm getActorFilm(@RequestParam(value = "actor", defaultValue = "Tom Hanks") final String actor) {
        return this.actorService.getActorFilm(actor);
    }
}
