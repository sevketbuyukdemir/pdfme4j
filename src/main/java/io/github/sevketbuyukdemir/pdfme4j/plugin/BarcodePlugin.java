package io.github.sevketbuyukdemir.pdfme4j.plugin;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class BarcodePlugin implements IPdfmePlugin {

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        if (value == null || value.isEmpty()) return;

        float x = (float) item.getPosition().getX() * Generator.MM_TO_POINTS;
        float yMm = (float) item.getPosition().getY() * Generator.MM_TO_POINTS;
        float width = (float) item.getWidth() * Generator.MM_TO_POINTS;
        float height = (float) item.getHeight() * Generator.MM_TO_POINTS;
        float y = pageHeightPoints - yMm - height;

        float opacity = 1.0f;
        if (item.getOpacity() != null) {
            opacity = item.getOpacity().floatValue();
        }

        String type = item.getType();
        BarcodeFormat format = resolveFormat(type);
        boolean includeText = isOneDimensional(format);

        int targetWidth = includeText ? 1200 : 1000;
        int targetHeight = includeText ? 400 : 1000;

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(value, format, targetWidth, targetHeight);
        BufferedImage barcodeImage = createBarcodeImage(bitMatrix, value, includeText);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(barcodeImage, "PNG", baos);

        byte[] imageBytes = baos.toByteArray();
        PDImageXObject barcodePdfImage = PDImageXObject.createFromByteArray(document, imageBytes, "barcode");

        contentStream.saveGraphicsState();

        if (opacity < 1.0f) {
            PDExtendedGraphicsState extGState = new PDExtendedGraphicsState();
            extGState.setAlphaSourceFlag(true);
            extGState.setNonStrokingAlphaConstant(opacity);
            extGState.setStrokingAlphaConstant(opacity);
            contentStream.setGraphicsStateParameters(extGState);
        }

        contentStream.drawImage(barcodePdfImage, x, y, width, height);
        contentStream.restoreGraphicsState();
    }

    private BufferedImage createBarcodeImage(BitMatrix bitMatrix, String value, boolean includeText) {
        int barcodeWidth = bitMatrix.getWidth();
        int barcodeHeight = bitMatrix.getHeight();
        int textHeight = includeText ? 50 : 0;

        BufferedImage image = new BufferedImage(
                barcodeWidth,
                barcodeHeight + textHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        graphics.setColor(Color.BLACK);

        for (int x = 0; x < barcodeWidth; x++) {
            for (int y = 0; y < barcodeHeight; y++) {
                if (bitMatrix.get(x, y)) {
                    graphics.fillRect(x, y, 1, 1);
                }
            }
        }

        if (includeText) {
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 32));

            int textWidth = graphics.getFontMetrics().stringWidth(value);
            int textX = Math.max(0, (barcodeWidth - textWidth) / 2);
            int textY = barcodeHeight + 36;

            graphics.drawString(value, textX, textY);
        }

        graphics.dispose();

        return image;
    }

    private BarcodeFormat resolveFormat(String type) {
        return switch (type) {
            case "qrcode" -> BarcodeFormat.QR_CODE;
            case "ean13" -> BarcodeFormat.EAN_13;
            case "ean8" -> BarcodeFormat.EAN_8;
            case "code39" -> BarcodeFormat.CODE_39;
            case "code128" -> BarcodeFormat.CODE_128;
            case "itf14" -> BarcodeFormat.ITF;
            case "upca" -> BarcodeFormat.UPC_A;
            case "upce" -> BarcodeFormat.UPC_E;
            case "pdf417" -> BarcodeFormat.PDF_417;
            case "datamatrix", "gs1datamatrix" -> BarcodeFormat.DATA_MATRIX;
            case "codabar", "nw7" -> BarcodeFormat.CODABAR;
            default -> throw new IllegalArgumentException("Unsupported barcode type: " + type);
        };
    }

    private boolean isOneDimensional(BarcodeFormat format) {
        return format == BarcodeFormat.EAN_13
                || format == BarcodeFormat.EAN_8
                || format == BarcodeFormat.CODE_39
                || format == BarcodeFormat.CODE_128
                || format == BarcodeFormat.ITF
                || format == BarcodeFormat.UPC_A
                || format == BarcodeFormat.UPC_E
                || format == BarcodeFormat.CODABAR;
    }
}