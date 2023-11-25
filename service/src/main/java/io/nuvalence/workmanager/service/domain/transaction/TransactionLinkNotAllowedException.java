package io.nuvalence.workmanager.service.domain.transaction;

import java.util.UUID;

/**
 * TransactionLinkNotAllowedException thrown when linking transactions not allowed.
 */
public class TransactionLinkNotAllowedException extends Exception {

    private static final long serialVersionUID = 7740282864681895752L;

    /**
     * Transactions attempted to link not allowed.
     *
     * @param from transaction attempted
     * @param to   transaction attempted
     */
    public TransactionLinkNotAllowedException(final UUID from, final UUID to) {
        super(
                String.format(
                        "Linking not been allowed between transactions [%s] and [%s]", from, to));
    }
}
