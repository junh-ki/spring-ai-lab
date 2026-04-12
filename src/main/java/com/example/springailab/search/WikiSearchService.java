package com.example.springailab.search;

import org.springframework.stereotype.Service;

@Service
public class WikiSearchService implements SearchService {

    @Override
    public String search(final String query) {
        return query;
    }
}
