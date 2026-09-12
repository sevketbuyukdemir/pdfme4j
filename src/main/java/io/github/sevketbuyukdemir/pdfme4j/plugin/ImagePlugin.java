package io.github.sevketbuyukdemir.pdfme4j.plugin;

import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.util.Base64;

public class ImagePlugin implements IPdfmePlugin {

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        if (value == null || value.isEmpty()) return;

        String base64Data = value;
        if (base64Data.contains(",")) {
            base64Data = base64Data.split(",")[1];
        }
        base64Data = base64Data.replaceAll("\\s", "");

        float opacity = 1.0f;
        if (item.getOpacity() != null) {
            opacity = item.getOpacity().floatValue();
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "img");

            float x = (float) item.getPosition().getX() * Generator.MM_TO_POINTS;
            float yMm = (float) item.getPosition().getY() * Generator.MM_TO_POINTS;
            float width = (float) item.getWidth() * Generator.MM_TO_POINTS;
            float height = (float) item.getHeight() * Generator.MM_TO_POINTS;

            float y = pageHeightPoints - yMm - height;

            contentStream.saveGraphicsState();

            if (opacity < 1.0f) {
                PDExtendedGraphicsState extGState = new PDExtendedGraphicsState();
                extGState.setAlphaSourceFlag(true);
                extGState.setNonStrokingAlphaConstant(opacity);
                extGState.setStrokingAlphaConstant(opacity);
                contentStream.setGraphicsStateParameters(extGState);
            }

            contentStream.drawImage(image, x, y, width, height);

            contentStream.restoreGraphicsState();
        } catch (Exception e) {
            // Do nothing
        }
    }
}
