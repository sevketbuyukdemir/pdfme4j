package io.github.sevketbuyukdemir.pdfme4j.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.util.Map;

public class MultiVariableTextPlugin implements IPdfmePlugin {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TextPlugin textPlugin = new TextPlugin();

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        String templateText = (String) item.getProp("text");
        if (templateText == null || templateText.isEmpty()) return;

        if (value != null && !value.isEmpty() && value.trim().startsWith("{")) {
            try {
                Map<String, Object> variables = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
                });
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    templateText = templateText.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        textPlugin.render(document, contentStream, item, templateText, pageHeightPoints);
    }
}
