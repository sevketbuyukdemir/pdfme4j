package io.github.sevketbuyukdemir.pdfme4j.plugin;

import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;

public class ShapePlugin implements IPdfmePlugin {

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        String type = item.getType();
        String colorHex = "#000000";
        float strokeWidth = 1.0f;
        float opacity = 1.0f;

        if (item.getProp("color") != null) colorHex = (String) item.getProp("color");
        if (item.getProp("strokeWidth") != null) strokeWidth = ((Number) item.getProp("strokeWidth")).floatValue();
        if (item.getOpacity() != null) opacity = item.getOpacity().floatValue();

        float x = (float) item.getPosition().getX() * Generator.MM_TO_POINTS;
        float yMm = (float) item.getPosition().getY() * Generator.MM_TO_POINTS;
        float width = (float) item.getWidth() * Generator.MM_TO_POINTS;
        float height = (float) item.getHeight() * Generator.MM_TO_POINTS;

        float y = pageHeightPoints - yMm - height;
        Color color = Color.decode(colorHex);

        contentStream.saveGraphicsState();

        if (opacity < 1.0f) {
            PDExtendedGraphicsState extGState = new PDExtendedGraphicsState();
            extGState.setAlphaSourceFlag(true);
            extGState.setNonStrokingAlphaConstant(opacity);
            extGState.setStrokingAlphaConstant(opacity);
            contentStream.setGraphicsStateParameters(extGState);
        }

        contentStream.setLineWidth(strokeWidth);

        if ("line".equals(type)) {
            contentStream.setStrokingColor(color);
            float lineY = y + height - (strokeWidth / 2f);
            contentStream.moveTo(x, lineY);
            contentStream.lineTo(x + width, lineY);
            contentStream.stroke();
        } else if ("rectangle".equals(type)) {
            contentStream.setNonStrokingColor(color);
            contentStream.addRect(x, y, width, height);
            contentStream.fill();
        } else if ("circle".equals(type)) {
            contentStream.setNonStrokingColor(color);
            float cx = x + width / 2f;
            float cy = y + height / 2f;
            float rx = width / 2f;
            float ry = height / 2f;

            float k = 0.552284749831f;
            float ox = rx * k;
            float oy = ry * k;

            contentStream.moveTo(cx - rx, cy);
            contentStream.curveTo(cx - rx, cy + oy, cx - ox, cy + ry, cx, cy + ry);
            contentStream.curveTo(cx + ox, cy + ry, cx + rx, cy + oy, cx + rx, cy);
            contentStream.curveTo(cx + rx, cy - oy, cx + ox, cy - ry, cx, cy - ry);
            contentStream.curveTo(cx - ox, cy - ry, cx - rx, cy - oy, cx - rx, cy);
            contentStream.fill();
        }

        contentStream.restoreGraphicsState();
    }
}
