package io.github.sevketbuyukdemir.pdfme4j.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TablePlugin implements IPdfmePlugin {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        if (value == null || value.isEmpty()) return;

        float startX = (float) item.getPosition().getX() * Generator.MM_TO_POINTS;
        float yMm = (float) item.getPosition().getY() * Generator.MM_TO_POINTS;
        float totalWidth = (float) item.getWidth() * Generator.MM_TO_POINTS;
        float currentY = pageHeightPoints - yMm;

        File fallbackFont = new File("C:/Windows/Fonts/arial.ttf");
        if (!fallbackFont.exists()) fallbackFont = new File("/Library/Fonts/Arial.ttf");
        PDFont font = fallbackFont.exists() ? PDType0Font.load(document, fallbackFont) :
                new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);

        List<List<String>> bodyData = objectMapper.readValue(value, new TypeReference<List<List<String>>>() {
        });

        List<String> headers = new ArrayList<>();
        if (item.getProp("head") instanceof List<?> headList) {
            for (Object obj : headList) headers.add(String.valueOf(obj));
        }

        List<Double> percentages = new ArrayList<>();
        if (item.getProp("headWidthPercentages") instanceof List<?> pList) {
            for (Object obj : pList) percentages.add(((Number) obj).doubleValue());
        }

        int colCount = headers.isEmpty() ? (!bodyData.isEmpty() ? bodyData.get(0).size() : 1) : headers.size();
        float[] colWidths = new float[colCount];
        for (int i = 0; i < colCount; i++) {
            if (i < percentages.size()) {
                colWidths[i] = (float) (totalWidth * (percentages.get(i) / 100.0));
            } else {
                colWidths[i] = totalWidth / colCount;
            }
        }

        List<List<String>> allRows = new ArrayList<>();
        boolean hasHead = item.getProp("showHead") == null || (Boolean) item.getProp("showHead");
        if (hasHead && !headers.isEmpty()) {
            allRows.add(headers);
        }
        allRows.addAll(bodyData);

        float cellHeight = 26f;

        contentStream.saveGraphicsState();

        for (int i = 0; i < allRows.size(); i++) {
            List<String> row = allRows.get(i);
            boolean isHeader = hasHead && (i == 0);

            contentStream.setLineWidth(0.5f);
            contentStream.setStrokingColor(Color.BLACK);

            if (isHeader) {
                contentStream.moveTo(startX, currentY);
                contentStream.lineTo(startX + totalWidth, currentY);
                contentStream.stroke();
            }

            float currentX = startX;
            for (int j = 0; j < Math.min(colCount, row.size()); j++) {
                String cellText = row.get(j) != null ? row.get(j) : "";

                contentStream.beginText();
                contentStream.setFont(font, isHeader ? 11 : 10);
                contentStream.setNonStrokingColor(Color.BLACK);

                float textWidth = font.getStringWidth(cellText) / 1000f * (isHeader ? 11 : 10);
                float offsetX = 5f;

                if (j == 3 || (j == colCount - 1)) {
                    offsetX = colWidths[j] - textWidth - 5f;
                } else if (j > 0 && j < 3) {
                    offsetX = (colWidths[j] - textWidth) / 2f;
                }

                contentStream.newLineAtOffset(currentX + offsetX, currentY - cellHeight + 10f);
                contentStream.showText(cellText);
                contentStream.endText();

                currentX += colWidths[j];
            }

            contentStream.moveTo(startX, currentY - cellHeight);
            contentStream.lineTo(startX + totalWidth, currentY - cellHeight);
            contentStream.stroke();

            currentY -= cellHeight;
        }

        contentStream.restoreGraphicsState();
    }
}
