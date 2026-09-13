package io.github.sevketbuyukdemir.pdfme4j.plugin;

import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TextPlugin implements IPdfmePlugin {
    private Generator generatorContext;

    public void setGeneratorContext(Generator generatorContext) {
        this.generatorContext = generatorContext;
    }

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        if (document == null) {
            throw new IllegalArgumentException("Document must not be null.");
        }

        if (contentStream == null) {
            throw new IllegalArgumentException("Content stream must not be null.");
        }

        if (item == null) {
            throw new IllegalArgumentException("Schema item must not be null.");
        }

        String text = value == null ? "" : value;

        if (text.isEmpty()) {
            return;
        }

        PDFont font = resolveFont(document, item);

        float fontSize = resolveFontSize(item);
        float lineHeight = resolveLineHeight(item, fontSize);

        float x = mmToPoints(item.getPosition().getX());
        float y = mmToPoints(item.getPosition().getY());

        float width = mmToPoints(item.getWidth());
        float height = mmToPoints(item.getHeight());

        float paddingLeft = resolvePadding(item, "left");
        float paddingRight = resolvePadding(item, "right");
        float paddingTop = resolvePadding(item, "top");
        float paddingBottom = resolvePadding(item, "bottom");

        float contentWidth = Math.max(0, width - paddingLeft - paddingRight);
        float contentHeight = Math.max(0, height - paddingTop - paddingBottom);

        Color fontColor = parseColor(item.getFontColor(), Color.BLACK);
        Color backgroundColor = parseColor(item.getBackgroundColor(), null);

        float opacity = resolveOpacity(item);

        if (backgroundColor != null) {
            drawBackground(contentStream, x, pageHeightPoints - y - height, width, height, backgroundColor, opacity);
        }

        String renderableText = filterUnsupportedGlyphs(text, font);

        if (renderableText.isEmpty()) {
            return;
        }

        float characterSpacing = resolveCharacterSpacing(item);

        List<String> lines = wrapText(renderableText, font, fontSize, contentWidth, characterSpacing);

        if (lines.isEmpty()) {
            return;
        }

        float totalLineHeight = lines.size() * lineHeight;

        FontMetrics fontMetrics = resolveFontMetrics(font, fontSize);

        float contentTop = pageHeightPoints - y - paddingTop;
        float contentBottom = pageHeightPoints - y - height + paddingBottom;

        float firstBaseline = calculateFirstBaseline(item, contentTop, contentBottom, contentHeight, totalLineHeight, fontMetrics);

        float rotation = item.getRotate() == null ? 0f : item.getRotate().floatValue();

        float centerX = x + width / 2f;
        float centerY = pageHeightPoints - y - height / 2f;

        contentStream.saveGraphicsState();

        try {
            applyOpacity(contentStream, opacity);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.setNonStrokingColor(fontColor);

            if (characterSpacing != 0f) {
                contentStream.setCharacterSpacing(characterSpacing);
            }

            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                float lineWidth = calculateTextWidth(font, line, fontSize, characterSpacing);
                float textX = calculateHorizontalStartX(item, x, width, paddingLeft, paddingRight, lineWidth);
                float textY = firstBaseline - index * lineHeight;

                if (rotation == 0f) {
                    contentStream.setTextMatrix(Matrix.getTranslateInstance(textX, textY));
                } else {
                    Point rotatedPoint = rotatePoint(textX, textY, centerX, centerY, -rotation);
                    contentStream.setTextMatrix(Matrix.getRotateInstance((float) Math.toRadians(-rotation), rotatedPoint.x, rotatedPoint.y));
                }

                if (!line.isEmpty()) {
                    contentStream.showText(line);
                }
            }

            contentStream.endText();
        } finally {
            contentStream.restoreGraphicsState();
        }
    }

    private FontMetrics resolveFontMetrics(PDFont font, float fontSize) {
        PDFontDescriptor descriptor = font.getFontDescriptor();

        float ascent = 0f;
        float descent = 0f;

        if (descriptor != null) {
            ascent = descriptor.getAscent() / 1000f * fontSize;
            descent = descriptor.getDescent() / 1000f * fontSize;
        }

        if (ascent <= 0f) {
            ascent = fontSize * 0.8f;
        }

        if (descent >= 0f) {
            descent = -fontSize * 0.2f;
        }

        return new FontMetrics(ascent, descent);
    }

    private float calculateFirstBaseline(SchemaItem item, float contentTop, float contentBottom, float contentHeight, float totalLineHeight, FontMetrics metrics) {
        String alignment = item.getVerticalAlignment();

        if (alignment == null || alignment.isBlank()) {
            alignment = "top";
        }

        alignment = alignment.toLowerCase(Locale.ROOT);

        float firstLineTop = switch (alignment) {
            case "middle", "center" -> contentBottom + (contentHeight + totalLineHeight) / 2f;
            case "bottom" -> contentBottom + totalLineHeight;
            default -> contentTop;
        };

        return firstLineTop - metrics.ascent;
    }

    private Point rotatePoint(float x, float y, float centerX, float centerY, float degrees) {
        double radians = Math.toRadians(degrees);

        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        float translatedX = x - centerX;
        float translatedY = y - centerY;
        float rotatedX = (float) (translatedX * cos - translatedY * sin);
        float rotatedY = (float) (translatedX * sin + translatedY * cos);

        return new Point(centerX + rotatedX, centerY + rotatedY);
    }

    private String filterUnsupportedGlyphs(String text, PDFont font) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        text.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));

            try {
                font.encode(character);
                result.append(character);
            } catch (IllegalArgumentException | IOException ignored) {
            }
        });

        return result.toString();
    }

    private PDFont resolveFont(PDDocument document, SchemaItem item) throws IOException {
        String fontName = item.getFontName();

        if (fontName != null && !fontName.isBlank() && generatorContext != null) {
            Map<String, File> customFonts = generatorContext.getCustomFonts();
            File fontFile = customFonts == null ? null : customFonts.get(fontName);

            if (fontFile != null && fontFile.exists() && fontFile.isFile()) {
                return PDType0Font.load(document, fontFile);
            }
        }

        return resolveSystemFont(document);
    }

    private PDFont resolveSystemFont(PDDocument document) throws IOException {
        FontMapper fontMapper = FontMappers.instance();
        FontMapping<org.apache.fontbox.ttf.TrueTypeFont> mapping = fontMapper.getTrueTypeFont("Arial", null);

        if (mapping == null || mapping.getFont() == null) {
            throw new IllegalStateException("Unable to resolve a system font.");
        }

        return PDType0Font.load(document, mapping.getFont(), true);
    }

    private float resolveFontSize(SchemaItem item) {
        Object dynamicFontSize = item.getProp("dynamicFontSize");

        if (!(dynamicFontSize instanceof Map<?, ?> config)) {
            return item.getFontSize();
        }

        float min = toFloat(config.get("min"), item.getFontSize());
        float max = toFloat(config.get("max"), item.getFontSize());

        if (max < min) {
            float temporary = min;
            min = max;
            max = temporary;
        }

        return Math.max(min, Math.min(item.getFontSize(), max));
    }

    private float resolveLineHeight(SchemaItem item, float fontSize) {

        float lineHeight = item.getLineHeight();

        if (lineHeight <= 0f) {
            return fontSize;
        }

        if (lineHeight <= 4f) {
            return fontSize * lineHeight;
        }

        return lineHeight;
    }

    private float resolveCharacterSpacing(SchemaItem item) {
        return item.getCharacterSpacing();
    }

    private float resolveOpacity(SchemaItem item) {

        if (item.getOpacity() == null) {
            return 1f;
        }

        return Math.max(0f, Math.min(1f, item.getOpacity().floatValue()));
    }

    private void applyOpacity(PDPageContentStream contentStream, float opacity) throws IOException {
        if (opacity >= 1f) {
            return;
        }

        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        graphicsState.setNonStrokingAlphaConstant(opacity);
        graphicsState.setStrokingAlphaConstant(opacity);
        contentStream.setGraphicsStateParameters(graphicsState);
    }

    private float resolvePadding(SchemaItem item, String side) {
        Object padding = item.getProp("padding");

        if (padding == null) {
            return 0f;
        }

        if (padding instanceof Number) {
            return mmToPoints(((Number) padding).doubleValue());
        }

        if (padding instanceof String) {
            return mmToPoints(parseFloat((String) padding, 0f));
        }

        if (!(padding instanceof Map<?, ?> paddingMap)) {
            return 0f;
        }

        Object sideValue = paddingMap.get(side);

        if (sideValue == null) {
            sideValue = paddingMap.get("vertical");
        }

        if (sideValue == null) {
            sideValue = paddingMap.get("horizontal");
        }

        if (sideValue == null) {
            sideValue = paddingMap.get("all");
        }

        return mmToPoints(toFloat(sideValue, 0f));
    }

    private float calculateHorizontalStartX(SchemaItem item, float x, float width, float paddingLeft, float paddingRight, float lineWidth) {
        String alignment = item.getAlignment();
        if (alignment == null || alignment.isBlank()) {
            alignment = "left";
        }

        alignment = alignment.toLowerCase(Locale.ROOT);

        float contentWidth = Math.max(0f, width - paddingLeft - paddingRight);

        return switch (alignment) {
            case "center" -> x + paddingLeft + (contentWidth - lineWidth) / 2f;
            case "right" -> x + width - paddingRight - lineWidth;
            default -> x + paddingLeft;
        };
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth, float characterSpacing) throws IOException {

        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {

            return lines;
        }

        if (maxWidth <= 0f) {
            lines.add(text);
            return lines;
        }

        String[] paragraphs = text.split("\\R", -1);

        for (String paragraph : paragraphs) {

            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split(" ");

            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                float candidateWidth = calculateTextWidth(font, candidate, fontSize, characterSpacing);

                if (candidateWidth <= maxWidth) {
                    currentLine = new StringBuilder(candidate);
                    continue;
                }

                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }

                currentLine = new StringBuilder(word);
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    private float calculateTextWidth(PDFont font, String text, float fontSize, float characterSpacing) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0f;
        }

        float width = font.getStringWidth(text) / 1000f * fontSize;

        if (characterSpacing != 0f && text.length() > 1) {
            width += characterSpacing * (text.length() - 1);
        }

        return width;
    }

    private void drawBackground(PDPageContentStream contentStream, float x, float y, float width, float height, Color color, float opacity) throws IOException {
        contentStream.saveGraphicsState();
        try {
            applyOpacity(contentStream, opacity);
            contentStream.setNonStrokingColor(color);
            contentStream.addRect(x, y, width, height);
            contentStream.fill();
        } finally {
            contentStream.restoreGraphicsState();
        }
    }

    private Color parseColor(String value, Color defaultColor) {
        if (value == null || value.isBlank()) {
            return defaultColor;
        }

        String colorValue = value.trim();
        try {
            if (colorValue.startsWith("#")) {
                return Color.decode(colorValue);
            }
            if (colorValue.startsWith("rgb")) {
                String numbers = colorValue.replace("rgba", "").replace("rgb", "").replace("(", "").replace(")", "");
                String[] parts = numbers.split(",");
                return new Color(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
            }
            return switch (colorValue.toLowerCase(Locale.ROOT)) {
                case "black" -> Color.BLACK;
                case "white" -> Color.WHITE;
                case "red" -> Color.RED;
                case "green" -> Color.GREEN;
                case "blue" -> Color.BLUE;
                case "gray", "grey" -> Color.GRAY;
                case "yellow" -> Color.YELLOW;
                default -> defaultColor;
            };
        } catch (RuntimeException exception) {
            return defaultColor;
        }
    }

    private float mmToPoints(double millimeters) {
        return (float) (millimeters * 72.0 / 25.4);
    }

    private float toFloat(Object value, float defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        if (value instanceof String) {
            return parseFloat((String) value, defaultValue);
        }

        return defaultValue;
    }

    private float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value.trim());
        } catch (RuntimeException exception) {
            return defaultValue;
        }
    }

    private static final class FontMetrics {
        private final float ascent;
        private final float descent;

        private FontMetrics(float ascent, float descent) {
            this.ascent = ascent;
            this.descent = descent;
        }
    }

    private static final class Point {
        private final float x;
        private final float y;

        private Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}