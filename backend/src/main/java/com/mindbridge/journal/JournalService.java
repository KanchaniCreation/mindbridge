package com.mindbridge.journal;

import com.mindbridge.common.ApiException;
import com.mindbridge.common.PageResponse;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every method receives the user id from the authenticated session and uses it in the query,
 * so a user can never read, change or delete someone else's entry.
 */
@Service
public class JournalService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 100;

    private final JournalEntryRepository entries;

    public JournalService(JournalEntryRepository entries) {
        this.entries = entries;
    }

    @Transactional
    public JournalEntryResponse create(UUID userId, JournalEntryRequest request) {
        JournalEntry entry = new JournalEntry(
            userId, clean(request.title()), clean(request.category()), request.body().trim());
        return JournalEntryResponse.from(entries.save(entry));
    }

    @Transactional(readOnly = true)
    public JournalEntryResponse get(UUID userId, UUID entryId) {
        return JournalEntryResponse.from(find(userId, entryId));
    }

    @Transactional
    public JournalEntryResponse update(UUID userId, UUID entryId, JournalEntryRequest request) {
        JournalEntry entry = find(userId, entryId);
        entry.update(clean(request.title()), clean(request.category()), request.body().trim());
        return JournalEntryResponse.from(entry);
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        entries.delete(find(userId, entryId));
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalListItem> list(UUID userId, String q, String category, int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (query.length() > MAX_SEARCH_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Search text is too long.");
        }
        String categoryFilter = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(
            entries.search(userId, categoryFilter, query, "%" + query + "%", pageable)
                .map(JournalListItem::from));
    }

    public List<String> prompts() {
        return JournalPrompts.ALL;
    }

    /** Unknown id and someone else's id look identical (404), so existence is not leaked. */
    private JournalEntry find(UUID userId, UUID entryId) {
        return entries.findByIdAndUserId(entryId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Journal entry not found."));
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
