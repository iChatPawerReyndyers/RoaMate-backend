package com.roamate.sync.apply;

import tools.jackson.databind.json.JsonMapper;
import com.roamate.finance.SettlementService;
import com.roamate.finance.dto.CreateExpenseRequest;
import com.roamate.sync.EventLogEntity;
import com.roamate.sync.EventType;
import org.springframework.stereotype.Component;

/**
 * Covers the direct "add expense" flow only (FinanceController.createExpense).
 * The checklist-to-expense conversion endpoint (ChecklistController.convertToExpense)
 * is a two-step operation - create the expense AND link it back onto the
 * checklist item - and isn't covered by this applier or queued offline yet;
 * that flow still calls its endpoint directly with no offline fallback.
 */
@Component
public class ExpenseCreatedApplier implements EventApplier {

    private final SettlementService settlementService;
    private final JsonMapper jsonMapper;

    public ExpenseCreatedApplier(SettlementService settlementService, JsonMapper jsonMapper) {
        this.settlementService = settlementService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EventType supportedType() {
        return EventType.EXPENSE_CREATED;
    }

    @Override
    public void apply(EventLogEntity event) throws Exception {
        CreateExpenseRequest request = jsonMapper.readValue(event.getPayloadJson(), CreateExpenseRequest.class);
        settlementService.createExpense(request);
    }
}
