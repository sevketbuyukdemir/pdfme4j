package io.github.sevketbuyukdemir.pdfme4j.plugin;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

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

        BarcodeFormat format = BarcodeFormat.QR_CODE;
        int targetWidth = 250;
        int targetHeight = 250;

        if ("barcodes".equals(item.getType())) {
            format = BarcodeFormat.CODE_128;
            targetWidth = 500;
            targetHeight = 150;
        }

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(value, format, targetWidth, targetHeight);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos, config);
        byte[] imageBytes = baos.toByteArray();

        PDImageXObject barcodeImage = PDImageXObject.createFromByteArray(document, imageBytes, "barcode");

        contentStream.saveGraphicsState();

        if (opacity < 1.0f) {
            PDExtendedGraphicsState extGState = new PDExtendedGraphicsState();
            extGState.setAlphaSourceFlag(true);
            extGState.setNonStrokingAlphaConstant(opacity);
            extGState.setStrokingAlphaConstant(opacity);
            contentStream.setGraphicsStateParameters(extGState);
        }

        contentStream.drawImage(barcodeImage, x, y, width, height);

        contentStream.restoreGraphicsState();
    }
}
