package io.github.sevketbuyukdemir.pdfme4j.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import io.github.sevketbuyukdemir.pdfme4j.model.Template;
import io.github.sevketbuyukdemir.pdfme4j.plugin.*;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {
    public static final float MM_TO_POINTS = 2.83464566929f;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, IPdfmePlugin> plugins = new HashMap<>();
    private final Map<String, File> customFonts = new HashMap<>();

    public Generator() {
        plugins.put("image", new ImagePlugin());
        plugins.put("text", new TextPlugin());
        plugins.put("signature", plugins.get("image"));

        MultiVariableTextPlugin mvPlugin = new MultiVariableTextPlugin();
        plugins.put("multiVariableText", mvPlugin);

        ShapePlugin shapePlugin = new ShapePlugin();
        plugins.put("line", shapePlugin);
        plugins.put("rectangle", shapePlugin);
        plugins.put("circle", shapePlugin);

        BarcodePlugin barcodePlugin = new BarcodePlugin();
        plugins.put("qrcode", barcodePlugin);
        plugins.put("barcode", barcodePlugin);
        plugins.put("barcodes", barcodePlugin);

        plugins.put("table", new TablePlugin());
        plugins.put("svg", new SvgPlugin());
    }

    public void registerPlugin(String type, IPdfmePlugin plugin) {
        this.plugins.put(type, plugin);
    }

    public void registerFont(String fontName, File ttfFile) {
        this.customFonts.put(fontName, ttfFile);
    }

    public Map<String, File> getCustomFonts() {
        return this.customFonts;
    }

    public byte[] generate(String templateJson, List<Map<String, String>> inputs) throws Exception {
        Template template = objectMapper.readValue(templateJson, Template.class);
        return generate(template, inputs);
    }

    public byte[] generate(Template template, List<Map<String, String>> inputs) throws Exception {
        try (PDDocument outputDocument = new PDDocument()) {
            PDDocument baseDocument = null;
            boolean hasBasePdf = false;
            String base64PdfString = null;

            if (template.getBasePdf() instanceof String base64Str) {
                if (!base64Str.equals("blank") && !base64Str.isEmpty()) {
                    hasBasePdf = true;
                    base64PdfString = base64Str;
                }
            } else if (template.getBasePdf() instanceof Map<?, ?> basePdfMap) {
                if (basePdfMap.containsKey("data")) {
                    hasBasePdf = true;
                    base64PdfString = (String) basePdfMap.get("data");
                }
            }

            if (hasBasePdf && base64PdfString != null) {
                if (base64PdfString.contains(",")) {
                    base64PdfString = base64PdfString.split(",")[1];
                }
                byte[] pdfBytes = Base64.getDecoder().decode(base64PdfString.replaceAll("\\s", ""));
                baseDocument = org.apache.pdfbox.Loader.loadPDF(pdfBytes);
            }

            for (Map<String, String> inputData : inputs) {
                for (int pageIndex = 0; pageIndex < template.getSchemas().size(); pageIndex++) {

                    float pageWidthMm = 210f;
                    float pageHeightMm = 297f;

                    if (template.getBasePdf() instanceof Map<?, ?> basePdfMap) {
                        if (basePdfMap.containsKey("width")) {
                            pageWidthMm = ((Number) basePdfMap.get("width")).floatValue();
                        }
                        if (basePdfMap.containsKey("height")) {
                            pageHeightMm = ((Number) basePdfMap.get("height")).floatValue();
                        }
                    }

                    if (hasBasePdf && baseDocument != null && pageIndex < baseDocument.getNumberOfPages()) {
                        PDRectangle baseMediaBox = baseDocument.getPage(pageIndex).getMediaBox();
                        pageWidthMm = baseMediaBox.getWidth() / MM_TO_POINTS;
                        pageHeightMm = baseMediaBox.getHeight() / MM_TO_POINTS;
                    }

                    PDPage page = new PDPage();
                    float pageHeightPoints = pageHeightMm * MM_TO_POINTS;
                    PDRectangle customSize = new PDRectangle(pageWidthMm * MM_TO_POINTS, pageHeightPoints);
                    page.setMediaBox(customSize);
                    outputDocument.addPage(page);

                    if (hasBasePdf && baseDocument != null && pageIndex < baseDocument.getNumberOfPages()) {
                        LayerUtility layerUtility = new LayerUtility(outputDocument);
                        PDFormXObject formXObject = layerUtility.importPageAsForm(baseDocument, pageIndex);

                        PDPage basePage = baseDocument.getPage(pageIndex);
                        float offsetX = basePage.getMediaBox().getLowerLeftX();
                        float offsetY = basePage.getMediaBox().getLowerLeftY();

                        try (PDPageContentStream backgroundStream = new PDPageContentStream(outputDocument, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                            backgroundStream.saveGraphicsState();
                            backgroundStream.transform(Matrix.getTranslateInstance(-offsetX, -offsetY));
                            backgroundStream.drawForm(formXObject);
                            backgroundStream.restoreGraphicsState();
                        }
                    }

                    List<Map<String, Object>> pageSchemasList = template.getSchemas().get(pageIndex);
                    try (PDPageContentStream contentStream = new PDPageContentStream(outputDocument, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        for (Map<String, Object> schemaRawMap : pageSchemasList) {
                            if (schemaRawMap == null || schemaRawMap.isEmpty()) continue;

                            SchemaItem item = null;
                            String fieldName = "";

                            if (schemaRawMap.containsKey("type")) {
                                item = objectMapper.convertValue(schemaRawMap, SchemaItem.class);
                                if (schemaRawMap.containsKey("name")) {
                                    fieldName = (String) schemaRawMap.get("name");
                                } else if (schemaRawMap.containsKey("id")) {
                                    fieldName = (String) schemaRawMap.get("id");
                                }
                            } else {
                                for (Object key : schemaRawMap.keySet()) {
                                    Object valueObj = schemaRawMap.get(key);
                                    if (valueObj instanceof Map<?, ?> m && m.containsKey("type")) {
                                        fieldName = (String) key;
                                        break;
                                    }
                                }
                                if (!fieldName.isEmpty()) {
                                    item = objectMapper.convertValue(schemaRawMap.get(fieldName), SchemaItem.class);
                                }
                            }

                            if (item == null) continue;

                            String value = item.getContent();
                            if (inputData != null && !fieldName.isEmpty() && inputData.containsKey(fieldName)) {
                                value = inputData.get(fieldName);
                            }

                            IPdfmePlugin plugin = plugins.get(item.getType());
                            if (plugin != null) {
                                if (plugin instanceof TextPlugin) {
                                    ((TextPlugin) plugin).setGeneratorContext(this);
                                }
                                plugin.render(outputDocument, contentStream, item, value, pageHeightPoints);
                            }
                        }
                    }
                }
            }

            if (baseDocument != null) baseDocument.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outputDocument.save(baos);
            return baos.toByteArray();
        }
    }
}
