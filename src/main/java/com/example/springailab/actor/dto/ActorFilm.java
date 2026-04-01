package com.example.springailab.actor.dto;

import java.util.List;

public record ActorFilm(String actor,
                        List<Movie> movies) {}
