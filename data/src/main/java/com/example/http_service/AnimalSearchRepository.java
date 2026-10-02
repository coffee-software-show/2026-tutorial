package com.example.http_service;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

interface AnimalSearchRepository extends ElasticsearchRepository<AnimalDocument, String> {

    /**
     * Full-text search across name and description. This is the thing Postgres can't do
     * well: relevance ranking, analysis, and fuzzy matching on typos.
     */
    @Query("""
            {
              "multi_match": {
                "query": "?0",
                "fields": [ "name^2", "description" ],
                "fuzziness": "AUTO"
              }
            }
            """)
    List<AnimalDocument> search(String query);

    /** Derived query, no JSON required — exact match on the keyword field. */
    List<AnimalDocument> findByType(String type);
}
