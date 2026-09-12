package io.github.sevketbuyukdemir.pdfme4j;

import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorTest {

    @Test
    public void testAllTemplatesInDirectory() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("templates");

        if (resource == null) {
            throw new java.io.FileNotFoundException("ERROR: src/test/resources/templates folder not found!");
        }

        File templatesDir = new File(resource.toURI());
        File[] jsonFiles = templatesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            return;
        }

        Generator generator = new Generator();
        File fontFile = new File("C:/Windows/Fonts/arial.ttf");
        if (fontFile.exists()) {
            generator.registerFont("Arial", fontFile);
            generator.registerFont("Roboto", fontFile);
        }

        for (File jsonFile : jsonFiles) {
            String fileName = jsonFile.getName();
            String pureName = fileName.substring(0, fileName.lastIndexOf('.'));
            String templateJson = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
            List<Map<String, String>> inputs = new ArrayList<>();
            inputs.add(new HashMap<>());
            byte[] pdfBytes = generator.generate(templateJson, inputs);
            assertNotNull(pdfBytes, fileName);
            assertTrue(pdfBytes.length > 0, fileName);
            String outputPath = "target/" + pureName + "_target.pdf";
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(pdfBytes);
            }
        }
    }
}
