package io.github.sevketbuyukdemir.pdfme4j.plugin;

import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

public interface IPdfmePlugin {
    void render(PDDocument document,
                PDPageContentStream contentStream,
                SchemaItem item,
                String value,
                float pageHeightPoints) throws Exception;
}

