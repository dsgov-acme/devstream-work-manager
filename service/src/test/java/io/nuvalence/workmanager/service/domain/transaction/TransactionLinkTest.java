package io.nuvalence.workmanager.service.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

public class TransactionLinkTest {

    @Test
    public void testEqualsSameReference() {
        TransactionLink transactionLink = new TransactionLink();
        assertTrue(
                transactionLink.equals(transactionLink),
                "A transaction link should be equal to itself.");
    }

    @Test
    public void testEqualsDifferentId() {
        TransactionLink transactionLink1 = new TransactionLink();
        transactionLink1.setId(UUID.randomUUID());

        TransactionLink transactionLink2 = new TransactionLink();
        transactionLink2.setId(UUID.randomUUID());

        assertFalse(
                transactionLink1.equals(transactionLink2),
                "Two transaction links with different IDs should not be equal.");
    }

    @Test
    public void testEqualsSameId() {
        UUID id = UUID.randomUUID();

        TransactionLink transactionLink1 = new TransactionLink();
        transactionLink1.setId(id);

        TransactionLink transactionLink2 = new TransactionLink();
        transactionLink2.setId(id);

        assertTrue(
                transactionLink1.equals(transactionLink2),
                "Two transaction links with the same ID should be equal.");
    }
}
