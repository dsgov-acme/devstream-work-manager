package io.nuvalence.workmanager.service.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of the NoteApiDelegate interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class NoteApiDelegateImpl /*implements NoteApiDelegate*/ {

    /*private final NoteService noteService;
    private final NoteMapper noteMapper;
    private final AuthorizationHandler authorizationHandler;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final TransactionAuditEventService transactionAuditEventService;
    private final RequestContextTimestamp requestContextTimestamp;

    @Override
    public ResponseEntity<NoteModelResponse> getTransactionNote(UUID transactionId, UUID noteId) {
        if (!authorizationHandler.isAllowed("view", TransactionNote.class)) {
            throw new ForbiddenException();
        }
        return ResponseEntity.ok(
                noteMapper.noteToNoteModelResponse(
                        noteService.getByTransactionIdAndId(transactionId, noteId)));
    }

    @Override
    public ResponseEntity<PagedTransactionNoteModel> getTransactionNotes(
            UUID transactionId,
            String startDate,
            String endDate,
            String type,
            Boolean includeDeleted,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", TransactionNote.class)) {
            throw new ForbiddenException();
        }

        TransactionNoteFilters filters =
                TransactionNoteFilters.builder()
                        .transactionId(transactionId)
                        .type(type)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .startDate(
                                OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeStartOfDay(startDate))
                        .endDate(OffsetDateTimeMapper.INSTANCE.toOffsetDateTimeEndOfDay(endDate))
                        .includeDeleted(includeDeleted)
                        .build();

        Page<NoteModelResponse> results =
                noteService
                        .getFilteredTransactionNotes(filters)
                        .map(noteMapper::noteToNoteModelResponse);

        return ResponseEntity.ok().body(generatePagedTransactionNoteModel(results));
    }

    @Override
    public ResponseEntity<NoteModelResponse> postTransactionNote(
            UUID id, NoteCreationModelRequest noteModelRequest) {

        if (!authorizationHandler.isAllowed("create", TransactionNote.class)) {
            throw new ForbiddenException();
        }

        if (noteModelRequest.getDocuments() != null) {
            noteModelRequest.getDocuments().stream()
                    .forEach(
                            document -> {
                                if (document == null) {
                                    throw new BusinessLogicException(
                                            "Provided document ids could not be mapped to UUIDs");
                                }
                            });
        }
        Note note =
                noteService.createTransactionNote(
                        id,
                        noteMapper.noteModelRequestToNote(noteModelRequest),
                        noteModelRequest.getType().getId());

        NoteModelResponse noteResponse = noteMapper.noteToNoteModelResponse(note);

        try {
            noteService.postAuditEventForTransactionNote(id, note, AuditActivityType.NOTE_ADDED);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "An error has occurred when recording an audit event for note creation"
                                    + " with id %s for transaction with id %s.",
                            note.getId(), id);
            log.error(errorMessage, e);
        }

        return ResponseEntity.ok(noteResponse);
    }

    @Override
    public ResponseEntity<NoteModelResponse> updateTransactionNote(
            UUID transactionId, UUID noteId, NoteUpdateModelRequest request) {

        if (!authorizationHandler.isAllowed("update", TransactionNote.class)) {
            throw new ForbiddenException();
        }

        Note requestNote = noteMapper.noteUpdateModelRequestToNote(request);
        requestNote.setId(noteId);

        TransactionNote existingNote =
                noteService.getByTransactionIdAndId(transactionId, requestNote.getId());

        try {
            final TransactionNote updatedTransactionNote =
                    AuditableAction.builder(TransactionNote.class)
                            .auditHandler(
                                    new TransactionNoteChangedAuditHandler(
                                            transactionAuditEventService))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    transactionNoteIn ->
                                            noteService.updateTransactionNote(
                                                    existingNote,
                                                    requestNote,
                                                    request.getType().getId()))
                            .build()
                            .execute(existingNote);

            NoteModelResponse noteResponse =
                    noteMapper.noteToNoteModelResponse(updatedTransactionNote);

            return ResponseEntity.ok(noteResponse);

        } catch (Exception e) {
            throw new UnexpectedException("", e);
        }
    }

    @Override
    public ResponseEntity<Void> softDeleteTransactionNote(UUID transactionId, UUID noteId) {
        if (!authorizationHandler.isAllowed("delete", TransactionNote.class)) {
            throw new ForbiddenException();
        }

        Note note = noteService.softDeleteTransactionNote(transactionId, noteId);

        try {
            noteService.postAuditEventForTransactionNote(
                    transactionId, note, AuditActivityType.NOTE_DELETED);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "An error has occurred when recording an audit event for note deletion"
                                    + " with id %s for transaction with id %s.",
                            noteId, transactionId);
            log.error(errorMessage, e);
        }

        return ResponseEntity.ok().build();
    }

    private PagedTransactionNoteModel generatePagedTransactionNoteModel(
            Page<NoteModelResponse> transactionNotes) {
        PagedTransactionNoteModel model = new PagedTransactionNoteModel();
        model.items(transactionNotes.toList());
        model.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(transactionNotes));
        return model;
    }*/
}
