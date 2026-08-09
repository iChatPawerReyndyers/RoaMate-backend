package com.roamate.finance;

import com.roamate.finance.domain.Expense;
import com.roamate.finance.domain.ExpenseParticipant;
import com.roamate.finance.domain.ExpensePayment;
import com.roamate.finance.dto.SettlementSummary;
import com.roamate.finance.repo.ExpenseRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

/** FIN-09: export the final ledger + settlement as PDF or CSV. */
@Service
public class ExportService {

    private final ExpenseRepository expenseRepository;
    private final SettlementService settlementService;

    public ExportService(ExpenseRepository expenseRepository, SettlementService settlementService) {
        this.expenseRepository = expenseRepository;
        this.settlementService = settlementService;
    }

    public byte[] exportCsv(UUID tripId) {
        List<Expense> expenses = expenseRepository.findByTripIdAndDeletedFalse(tripId);
        StringWriter sw = new StringWriter();
        sw.write("date,description,category,total,payers,participants\n");
        for (Expense e : expenses) {
            String payers = e.getPayments().stream()
                    .map(p -> (p.getPayerUserId() == null ? "KITTY" : p.getPayerUserId()) + ":" + p.getAmountPaid())
                    .reduce((a, b) -> a + "|" + b).orElse("");
            String participants = e.getParticipants().stream()
                    .map(ExpenseParticipant::getUserId)
                    .reduce((a, b) -> a + "|" + b).orElse("");
            sw.write(String.format("%s,\"%s\",%s,%s,%s,%s%n",
                    e.getExpenseDate(), e.getDescription().replace("\"", "\"\""),
                    e.getCategory(), e.getTotalAmount(), payers, participants));
        }
        return sw.toString().getBytes();
    }

    public byte[] exportPdf(UUID tripId) throws IOException {
        SettlementSummary summary = settlementService.computeSettlement(tripId);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cs.newLineAtOffset(50, 740);
                cs.showText("RoaMate Trip Settlement Report");
                cs.endText();

                float y = 700;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                for (var balance : summary.balances()) {
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(String.format("%s: paid $%.2f, owes $%.2f, net %s$%.2f",
                            balance.userId(),
                            balance.totalPaidCents() / 100.0,
                            balance.totalFairShareCents() / 100.0,
                            balance.netDeltaCents() >= 0 ? "+" : "-",
                            Math.abs(balance.netDeltaCents()) / 100.0));
                    cs.endText();
                    y -= 18;
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
