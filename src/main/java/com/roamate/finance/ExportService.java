package com.roamate.finance;

import com.roamate.activity.ActivitySession;
import com.roamate.activity.ActivitySessionRepository;
import com.roamate.finance.domain.Expense;
import com.roamate.finance.domain.ExpenseParticipant;
import com.roamate.finance.domain.KittyDeposit;
import com.roamate.finance.dto.SettlementSummary;
import com.roamate.finance.repo.ExpenseRepository;
import com.roamate.finance.repo.KittyDepositRepository;
import com.roamate.itinerary.LocationNote;
import com.roamate.itinerary.LocationNoteRepository;
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

/**
 * FIN-09: "Generates exportable summary reports containing total expenses,
 * individual deposit histories, Kitty usage, activity logs, location notes,
 * and final debt-settlement instructions." Originally only covered expenses
 * + settlement balances - the other three sections were missing from both
 * formats.
 */
@Service
public class ExportService {

    private final ExpenseRepository expenseRepository;
    private final SettlementService settlementService;
    private final KittyDepositRepository kittyDepositRepository;
    private final ActivitySessionRepository activitySessionRepository;
    private final LocationNoteRepository locationNoteRepository;

    public ExportService(ExpenseRepository expenseRepository,
                          SettlementService settlementService,
                          KittyDepositRepository kittyDepositRepository,
                          ActivitySessionRepository activitySessionRepository,
                          LocationNoteRepository locationNoteRepository) {
        this.expenseRepository = expenseRepository;
        this.settlementService = settlementService;
        this.kittyDepositRepository = kittyDepositRepository;
        this.activitySessionRepository = activitySessionRepository;
        this.locationNoteRepository = locationNoteRepository;
    }

    public byte[] exportCsv(UUID tripId) {
        StringWriter sw = new StringWriter();

        sw.write("## Expenses\n");
        sw.write("date,description,category,total,payers,participants\n");
        for (Expense e : expenseRepository.findByTripIdAndDeletedFalse(tripId)) {
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

        sw.write("\n## Kitty Deposits\n");
        sw.write("date,depositor,amount\n");
        for (KittyDeposit d : kittyDepositRepository.findByTripIdAndDeletedFalse(tripId)) {
            sw.write(String.format("%s,%s,%s%n", d.getDepositedAt(), d.getDepositorUserId(), d.getAmount()));
        }

        sw.write("\n## Activity Logs\n");
        sw.write("startedAt,user,type,steps,distanceMeters,elevationGainMeters,relativeDepthMeters,destinationId\n");
        for (ActivitySession s : activitySessionRepository.findByTripId(tripId)) {
            sw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    s.getStartedAt(), s.getUserId(), s.getType(),
                    nullToBlank(s.getStepCount()), nullToBlank(s.getDistanceMeters()),
                    nullToBlank(s.getElevationGainMeters()), nullToBlank(s.getRelativeDepthMeters()),
                    nullToBlank(s.getDestinationId())));
        }

        sw.write("\n## Location Notes\n");
        sw.write("createdAt,author,destinationId,note\n");
        for (LocationNote n : locationNoteRepository.findByTripId(tripId)) {
            sw.write(String.format("%s,%s,%s,\"%s\"%n",
                    n.getCreatedAt(), n.getAuthorUserId(), n.getDestination().getId(),
                    n.getBody().replace("\"", "\"\"")));
        }

        sw.write("\n## Settlement\n");
        sw.write("userId,totalPaidCents,totalFairShareCents,netDeltaCents\n");
        for (var balance : settlementService.computeSettlement(tripId).balances()) {
            sw.write(String.format("%s,%d,%d,%d%n",
                    balance.userId(), balance.totalPaidCents(), balance.totalFairShareCents(), balance.netDeltaCents()));
        }

        return sw.toString().getBytes();
    }

    public byte[] exportPdf(UUID tripId) throws IOException {
        SettlementSummary summary = settlementService.computeSettlement(tripId);
        List<KittyDeposit> deposits = kittyDepositRepository.findByTripIdAndDeletedFalse(tripId);
        List<ActivitySession> sessions = activitySessionRepository.findByTripId(tripId);
        List<LocationNote> notes = locationNoteRepository.findByTripId(tripId);

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSettlementPage(doc, summary);
            writeListPage(doc, "Kitty Deposits", deposits, d ->
                    String.format("%s  %s  %s", d.getDepositedAt(), d.getDepositorUserId(), d.getAmount()));
            writeListPage(doc, "Activity Logs", sessions, s ->
                    String.format("%s  %s  %s  steps=%s dist=%sm elev=%sm depth=%sm",
                            s.getStartedAt(), s.getUserId(), s.getType(),
                            nullToBlank(s.getStepCount()), nullToBlank(s.getDistanceMeters()),
                            nullToBlank(s.getElevationGainMeters()), nullToBlank(s.getRelativeDepthMeters())));
            writeListPage(doc, "Location Notes", notes, n ->
                    String.format("%s  %s: %s", n.getCreatedAt(), n.getAuthorUserId(), n.getBody()));

            doc.save(out);
            return out.toByteArray();
        }
    }

    private void writeSettlementPage(PDDocument doc, SettlementSummary summary) throws IOException {
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
    }

    /**
     * One section per call, starting a fresh page (and continuing onto
     * further fresh pages if a section runs past the bottom margin) -
     * simplest layout that won't overlap text given trip data volumes are
     * modest, at the cost of some blank space on partially-filled pages.
     */
    private <T> void writeListPage(PDDocument doc, String title, List<T> items, java.util.function.Function<T, String> formatter) throws IOException {
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        try {
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            cs.newLineAtOffset(50, 740);
            cs.showText(title);
            cs.endText();

            float y = 700;
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            if (items.isEmpty()) {
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText("(none)");
                cs.endText();
            }

            for (T item : items) {
                if (y < 50) {
                    cs.close();
                    page = new PDPage();
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    y = 740;
                }
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText(truncate(formatter.apply(item), 100));
                cs.endText();
                y -= 16;
            }
        } finally {
            cs.close();
        }
    }

    private static String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 3) + "..." : s;
    }

    private static String nullToBlank(Object value) {
        return value == null ? "" : value.toString();
    }
}
