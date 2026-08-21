package com.roamate.finance;

import com.roamate.finance.domain.Expense;
import com.roamate.finance.domain.KittyDeposit;
import com.roamate.finance.dto.CreateExpenseRequest;
import com.roamate.finance.dto.DuplicateGroupDto;
import com.roamate.finance.dto.ExpenseDto;
import com.roamate.finance.dto.SettlementSummary;
import com.roamate.finance.repo.KittyDepositRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final SettlementService settlementService;
    private final ExportService exportService;
    private final KittyDepositRepository kittyDepositRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    public FinanceController(SettlementService settlementService,
                             ExportService exportService,
                             KittyDepositRepository kittyDepositRepository,
                             DuplicateDetectionService duplicateDetectionService) {
        this.settlementService = settlementService;
        this.exportService = exportService;
        this.kittyDepositRepository = kittyDepositRepository;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @PostMapping("/expenses")
    public Expense createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        return settlementService.createExpense(request);
    }

    @PostMapping("/kitty-deposits")
    public KittyDeposit createDeposit(@RequestBody KittyDeposit deposit) {
        return kittyDepositRepository.save(deposit);
    }

    /** FIN-06: per-member deposit breakdown, e.g. "Alice: $2,000, Bob: $2,000, Charlie: $0". */
    @GetMapping("/trips/{tripId}/kitty-deposits")
    public List<KittyDeposit> listDeposits(@PathVariable UUID tripId) {
        return kittyDepositRepository.findByTripIdAndDeletedFalse(tripId);
    }

    /** FIN-08: everything currently sitting in the Conflict Review Dashboard for a trip. */
    @GetMapping("/trips/{tripId}/duplicates")
    public List<DuplicateGroupDto> listDuplicateGroups(@PathVariable UUID tripId) {
        return duplicateDetectionService.listDuplicateGroups(tripId);
    }

    /**
     * FIN-08: the check/uncheck action on a flagged entry. `keep=false`
     * soft-deletes it out of every budget/debt calculation; `keep=true`
     * restores it.
     */
    @PostMapping("/expenses/{expenseId}/resolve-duplicate")
    public Expense resolveDuplicate(@PathVariable UUID expenseId, @RequestParam boolean keep) {
        return duplicateDetectionService.resolveDuplicate(expenseId, keep);
    }

    @GetMapping("/trips/{tripId}/settlement")
    public SettlementSummary getSettlement(@PathVariable UUID tripId) {
        return settlementService.computeSettlement(tripId);
    }

    /**
     * FIN-05: raw per-expense list, mirrored by the mobile client into its
     * local cache (expensesRepository.ts) so FinanceSummaryScreen can fall
     * back to an offline-computed settlement (SettlementEngine.ts) when
     * getSettlement above isn't reachable.
     */
    @GetMapping("/trips/{tripId}/expenses")
    public List<ExpenseDto> listExpenses(@PathVariable UUID tripId) {
        return settlementService.listExpenses(tripId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping(value = "/trips/{tripId}/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID tripId) {
        byte[] csv = exportService.exportCsv(tripId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-export.csv")
                .body(csv);
    }

    @GetMapping(value = "/trips/{tripId}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID tripId) throws IOException {
        byte[] pdf = exportService.exportPdf(tripId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-settlement.pdf")
                .body(pdf);
    }

    private ExpenseDto toDto(Expense expense) {
        List<ExpenseDto.PaymentLineOut> payments = expense.getPayments().stream()
                .map(p -> new ExpenseDto.PaymentLineOut(p.getSource().name(), p.getPayerUserId(), p.getAmountPaid().cents()))
                .collect(Collectors.toList());
        List<ExpenseDto.ParticipantOut> participants = expense.getParticipants().stream()
                .map(p -> new ExpenseDto.ParticipantOut(p.getUserId(), p.getFairShare().cents()))
                .collect(Collectors.toList());
        return new ExpenseDto(
                expense.getId(), expense.getDescription(), expense.getTotalAmount().cents(), expense.getExpenseDate(),
                expense.getCategory(), expense.getCreatedByUserId(), expense.isFlaggedDuplicate(), payments, participants);
    }
}