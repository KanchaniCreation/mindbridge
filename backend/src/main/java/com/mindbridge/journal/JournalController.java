package com.mindbridge.journal;

import com.mindbridge.common.PageResponse;
import com.mindbridge.security.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journal")
public class JournalController {

    private final JournalService journal;

    public JournalController(JournalService journal) {
        this.journal = journal;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryResponse create(@AuthenticationPrincipal AppUserDetails me,
                                       @Valid @RequestBody JournalEntryRequest request) {
        return journal.create(me.getId(), request);
    }

    @GetMapping
    public PageResponse<JournalListItem> list(@AuthenticationPrincipal AppUserDetails me,
                                              @RequestParam(name = "q", required = false) String q,
                                              @RequestParam(name = "category", required = false) String category,
                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size) {
        return journal.list(me.getId(), q, category, page, size);
    }

    @GetMapping("/prompts")
    public List<String> prompts() {
        return journal.prompts();
    }

    @GetMapping("/{id}")
    public JournalEntryResponse get(@AuthenticationPrincipal AppUserDetails me,
                                    @PathVariable("id") UUID id) {
        return journal.get(me.getId(), id);
    }

    @PutMapping("/{id}")
    public JournalEntryResponse update(@AuthenticationPrincipal AppUserDetails me,
                                       @PathVariable("id") UUID id,
                                       @Valid @RequestBody JournalEntryRequest request) {
        return journal.update(me.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails me, @PathVariable("id") UUID id) {
        journal.delete(me.getId(), id);
    }
}
