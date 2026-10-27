package com.anele.sensitivewords;

import com.anele.sensitivewords.filter.MessageMasker;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MessageMaskerTest {
    private final MessageMasker masker = new MessageMasker();
    @Test void ignoresCaseAndPreservesPunctuation() {
        assertThat(masker.mask("select, SELECT! Drop?", List.of("SELECT", "DROP")))
            .isEqualTo("******, ******! ****?");
    }
    @Test void doesNotMaskInsideIdentifiersOrUnicodeWords() {
        assertThat(masker.mask("selection dropdown MY_SELECT éSELECT SELECTé SELECT_1 SELECT2", List.of("SELECT", "DROP")))
            .isEqualTo("selection dropdown MY_SELECT éSELECT SELECTé SELECT_1 SELECT2");
    }
    @Test void prioritizesLongestPhraseAndPreservesWhitespace() {
        assertThat(masker.mask("select \t*\nfrom customers", List.of("SELECT", "SELECT * FROM")))
            .isEqualTo("****** \t*\n**** customers");
    }
    @Test void treatsRegexCharactersLiterally() {
        assertThat(masker.mask("C++ a.b axb $DROP", List.of("C++", "a.b", "$DROP")))
            .isEqualTo("*** *** axb *****");
    }
    @Test void handlesRepeatedAndAdjacentMatches() {
        assertThat(masker.mask("DROP/DROP SELECT", List.of("DROP", "SELECT")))
            .isEqualTo("****/**** ******");
    }
    @Test void leavesEmptyAndUnmatchedInputUnchanged() {
        assertThat(masker.mask("", List.of("SELECT"))).isEmpty();
        assertThat(masker.mask("Hello 😊", List.of("SELECT"))).isEqualTo("Hello 😊");
        assertThat(masker.mask("SELECT", List.of())).isEqualTo("SELECT");
    }
    @Test void doesNotCorruptUnmatchedSupplementaryCharacters() {
        assertThat(masker.mask("😊 SELECT", List.of("SELECT"))).isEqualTo("😊 ******");
        assertThat(masker.mask("😊", List.of("😊"))).isEqualTo("*");
    }
    @Test void doesNotDependOnDictionaryOrder() {
        assertThat(masker.mask("CURRENT_USER USER", List.of("USER", "CURRENT_USER")))
            .isEqualTo("************ ****");
    }
}
