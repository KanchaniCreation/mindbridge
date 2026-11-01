package com.mindbridge.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class InviteCodesTest {

    @Test
    void generatedCodesHaveTheExpectedShapeAndAreNotRepeated() {
        String first = InviteCodes.generate();
        String second = InviteCodes.generate();

        assertThat(first).hasSize(10).matches("[A-Z2-9]+").doesNotContain("O", "I", "0", "1");
        assertThat(first).isNotEqualTo(second);
        assertThat(InviteCodes.format(first)).matches("[A-Z2-9]{5}-[A-Z2-9]{5}");
    }

    @Test
    void typedCodesAreNormalizedAndHashedConsistently() {
        String code = "ABCDEFGHJK";

        assertThat(InviteCodes.normalize("abcde-fghjk")).isEqualTo(code);
        assertThat(InviteCodes.normalize(" ABCDE FGHJK ")).isEqualTo(code);
        assertThat(InviteCodes.hash(code)).hasSize(64).isEqualTo(InviteCodes.hash("ABCDEFGHJK"));
        assertThat(InviteCodes.hash(code)).isNotEqualTo(InviteCodes.hash("ABCDEFGHJL"));
    }

    @Test
    void pseudonymPairsAreDifferentNames() {
        for (int i = 0; i < 50; i++) {
            List<String> pair = PseudonymGenerator.pair();
            assertThat(pair.get(0)).isNotEqualTo(pair.get(1));
        }
    }
}
