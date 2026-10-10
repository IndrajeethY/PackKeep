package in.indrajeeth.packkeep.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class ResidentPacksTest {
    private static final String A = "a".repeat(40);
    private static final String B = "b".repeat(40);
    private static final String UNNAMED = "\u0000";

    @BeforeEach
    void reset() {
        ResidentPacks.drop();
        ResidentPacks.consumeLeaving();
    }

    @Test
    void matchesLiveOnSameKeys() {
        ResidentPacks.markLive(List.of(A), "srv");
        assertTrue(ResidentPacks.matchesLive(List.of(A)));
        assertFalse(ResidentPacks.matchesLive(List.of(B)));
    }

    @Test
    void unnamedNeverMatches() {
        ResidentPacks.markLive(List.of(UNNAMED), "srv");
        assertFalse(ResidentPacks.matchesLive(List.of(UNNAMED)));
    }

    @Test
    void nothingLiveNeverMatches() {
        assertFalse(ResidentPacks.matchesLive(List.of(A)));
    }

    @Test
    void holdingOnlyWhenEmptyAndPacksLive() {
        assertFalse(ResidentPacks.holding(List.of()));
        ResidentPacks.markLive(List.of(A), "srv");
        assertTrue(ResidentPacks.holding(List.of()));
        assertFalse(ResidentPacks.holding(List.of(A)));
    }

    @Test
    void dropClearsLive() {
        ResidentPacks.markLive(List.of(A), "srv");
        ResidentPacks.drop();
        assertFalse(ResidentPacks.matchesLive(List.of(A)));
        assertFalse(ResidentPacks.holding(List.of()));
    }

    @Test
    void leavingIsConsumedOnce() {
        assertFalse(ResidentPacks.consumeLeaving());
        ResidentPacks.markLeaving();
        assertTrue(ResidentPacks.consumeLeaving());
        assertFalse(ResidentPacks.consumeLeaving());
    }

    @Test
    void identifiableRejectsUnnamed() {
        assertTrue(ResidentPacks.identifiable(List.of(A, B)));
        assertFalse(ResidentPacks.identifiable(List.of(A, UNNAMED)));
    }
}
