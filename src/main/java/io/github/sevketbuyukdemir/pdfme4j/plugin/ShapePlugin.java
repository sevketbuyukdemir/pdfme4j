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
        float opacity = item.getOpacity() != null ? item.getOpacity().floatValue() : 1.0f;

        float x = (float) item.getPosition().getX() * Generator.MM_TO_POINTS;
        float yMm = (float) item.getPosition().getY() * Generator.MM_TO_POINTS;
        float width = (float) item.getWidth() * Generator.MM_TO_POINTS;
        float height = (float) item.getHeight() * Generator.MM_TO_POINTS;
        float y = pageHeightPoints - yMm - height;
        float lineWidth = (float) item.getHeight() * Generator.MM_TO_POINTS;

        switch (type) {
            case "line" -> drawLine(contentStream, item, x, y, width, lineWidth, opacity);
            case "rectangle" -> drawRectangle(contentStream, item, x, y, width, height, opacity);
            case "ellipse" -> drawEllipse(contentStream, item, x, y, width, height, opacity);
        }
    }

    private void drawLine(PDPageContentStream contentStream, SchemaItem item, float x, float y, float width, float lineWidth, float opacity) throws Exception {
        String colorHex = getColor(item.getProp("color"));

        if (colorHex == null || width <= 0 || lineWidth <= 0) {
            return;
        }

        Color color = Color.decode(colorHex);
        float centerX = x + width / 2f;
        float centerY = y + lineWidth / 2f;
        float rotation = (float) Math.toRadians(-getRotation(item));
        float halfWidth = width / 2f;

        float[] start = rotatePoint(centerX - halfWidth, centerY, centerX, centerY, rotation);
        float[] end = rotatePoint(centerX + halfWidth, centerY, centerX, centerY, rotation);

        contentStream.saveGraphicsState();
        setOpacity(contentStream, opacity);

        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(lineWidth);
        contentStream.moveTo(start[0], start[1]);
        contentStream.lineTo(end[0], end[1]);
        contentStream.stroke();

        contentStream.restoreGraphicsState();
    }

    private void drawRectangle(PDPageContentStream contentStream, SchemaItem item, float x, float y, float width, float height, float opacity) throws Exception {
        String fillHex = getColor(item.getProp("color"));
        String borderHex = getColor(item.getProp("borderColor"));
        float borderWidth = getNumber(item.getProp("borderWidth")) * Generator.MM_TO_POINTS;

        if (fillHex == null && borderHex == null) {
            return;
        }

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float rotation = (float) Math.toRadians(-getRotation(item));

        float left = x + borderWidth / 2f;
        float bottom = y + borderWidth / 2f;
        float right = x + width - borderWidth / 2f;
        float top = y + height - borderWidth / 2f;

        float[] p1 = rotatePoint(left, bottom, centerX, centerY, rotation);
        float[] p2 = rotatePoint(right, bottom, centerX, centerY, rotation);
        float[] p3 = rotatePoint(right, top, centerX, centerY, rotation);
        float[] p4 = rotatePoint(left, top, centerX, centerY, rotation);

        contentStream.saveGraphicsState();
        setOpacity(contentStream, opacity);

        if (fillHex != null) {
            contentStream.setNonStrokingColor(Color.decode(fillHex));
        }

        if (borderHex != null && borderWidth > 0) {
            contentStream.setStrokingColor(Color.decode(borderHex));
            contentStream.setLineWidth(borderWidth);
        }

        contentStream.moveTo(p1[0], p1[1]);
        contentStream.lineTo(p2[0], p2[1]);
        contentStream.lineTo(p3[0], p3[1]);
        contentStream.lineTo(p4[0], p4[1]);
        contentStream.closePath();

        if (fillHex != null && borderHex != null && borderWidth > 0) {
            contentStream.fillAndStroke();
        } else if (fillHex != null) {
            contentStream.fill();
        } else if (borderWidth > 0) {
            contentStream.stroke();
        }

        contentStream.restoreGraphicsState();
    }

    private void drawEllipse(PDPageContentStream contentStream, SchemaItem item, float x, float y, float width, float height, float opacity) throws Exception {
        String fillHex = getColor(item.getProp("color"));
        String borderHex = getColor(item.getProp("borderColor"));
        float borderWidth = getNumber(item.getProp("borderWidth")) * Generator.MM_TO_POINTS;

        if (fillHex == null && borderHex == null) {
            return;
        }

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float radiusX = Math.max(0, width / 2f - borderWidth / 2f);
        float radiusY = Math.max(0, height / 2f - borderWidth / 2f);
        float rotation = (float) Math.toRadians(-getRotation(item));

        contentStream.saveGraphicsState();
        setOpacity(contentStream, opacity);

        if (fillHex != null) {
            contentStream.setNonStrokingColor(Color.decode(fillHex));
        }

        if (borderHex != null && borderWidth > 0) {
            contentStream.setStrokingColor(Color.decode(borderHex));
            contentStream.setLineWidth(borderWidth);
        }

        addEllipse(contentStream, centerX, centerY, radiusX, radiusY, rotation);

        if (fillHex != null && borderHex != null && borderWidth > 0) {
            contentStream.fillAndStroke();
        } else if (fillHex != null) {
            contentStream.fill();
        } else if (borderWidth > 0) {
            contentStream.stroke();
        }

        contentStream.restoreGraphicsState();
    }

    private void addEllipse(PDPageContentStream contentStream, float centerX, float centerY, float radiusX, float radiusY, float rotation) throws Exception {
        float k = 0.552284749831f;
        float ox = radiusX * k;
        float oy = radiusY * k;

        float[] p = rotatePoint(centerX + radiusX, centerY, centerX, centerY, rotation);
        contentStream.moveTo(p[0], p[1]);

        curveTo(contentStream, centerX + radiusX, centerY, centerX + radiusX, centerY + oy, centerX + ox, centerY + radiusY, centerX, centerY + radiusY, centerX, centerY, rotation);

        curveTo(contentStream, centerX, centerY + radiusY, centerX - ox, centerY + radiusY, centerX - radiusX, centerY + oy, centerX - radiusX, centerY, centerX, centerY, rotation);

        curveTo(contentStream, centerX - radiusX, centerY, centerX - radiusX, centerY - oy, centerX - ox, centerY - radiusY, centerX, centerY - radiusY, centerX, centerY, rotation);

        curveTo(contentStream, centerX, centerY - radiusY, centerX + ox, centerY - radiusY, centerX + radiusX, centerY - oy, centerX + radiusX, centerY, centerX, centerY, rotation);

        contentStream.closePath();
    }

    private void curveTo(PDPageContentStream contentStream, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float centerX, float centerY, float rotation) throws Exception {
        float[] p1 = rotatePoint(x2, y2, centerX, centerY, rotation);
        float[] p2 = rotatePoint(x3, y3, centerX, centerY, rotation);
        float[] p3 = rotatePoint(x4, y4, centerX, centerY, rotation);

        contentStream.curveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
    }

    private float[] rotatePoint(float x, float y, float centerX, float centerY, float rotation) {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);

        float translatedX = x - centerX;
        float translatedY = y - centerY;

        return new float[]{centerX + translatedX * cos - translatedY * sin, centerY + translatedX * sin + translatedY * cos};
    }

    private String getColor(Object value) {
        if (!(value instanceof String color) || color.isBlank()) {
            return null;
        }

        return color;
    }

    private float getNumber(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }

        return (float) 0;
    }

    private float getRotation(SchemaItem item) {
        return item.getRotate() != null ? item.getRotate().floatValue() : 0;
    }

    private void setOpacity(PDPageContentStream contentStream, float opacity) throws Exception {
        if (opacity >= 1.0f) {
            return;
        }

        PDExtendedGraphicsState extGState = new PDExtendedGraphicsState();
        extGState.setAlphaSourceFlag(true);
        extGState.setNonStrokingAlphaConstant(opacity);
        extGState.setStrokingAlphaConstant(opacity);
        contentStream.setGraphicsStateParameters(extGState);
    }
}