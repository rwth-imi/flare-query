package de.rwth.imi.flare.requestor.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceTest {

    @Test
    void id() {
        var reference = new Reference("Patient/125521");

        var id = reference.id();

        assertThat(id).isEqualTo("125521");
    }

    @Test
    void id_empty() {
        var reference = new Reference("");

        var id = reference.id();

        assertThat(id).isEmpty();
    }

    @Test
    void id_withoutSlash() {
        var reference = new Reference("a");

        var id = reference.id();

        assertThat(id).isEqualTo("a");
    }
}
