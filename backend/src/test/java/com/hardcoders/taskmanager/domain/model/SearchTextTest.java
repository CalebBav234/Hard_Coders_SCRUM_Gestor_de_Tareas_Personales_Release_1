package com.hardcoders.taskmanager.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextTest {

    @Test
    void removesAccentsLowercasesAndEscapesWildcards() {
        assertThat(SearchText.likePattern("  Reunión_50% ")).contains("%reunion\\_50\\%%");
    }

    @Test
    void blankQueriesDoNotFilter() {
        assertThat(SearchText.likePattern("   ")).isEmpty();
        assertThat(SearchText.likePattern(null)).isEmpty();
    }
}
