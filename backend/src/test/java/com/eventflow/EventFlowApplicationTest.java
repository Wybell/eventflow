package com.eventflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventFlowApplicationTest {

    @Test
    void shouldDeclareTheApplicationEntryPoint() {
        assertThat(EventFlowApplication.class).isNotNull();
    }
}
