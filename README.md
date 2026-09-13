# pdfme4j

**pdfme4j** is a pure Java port of [pdfme](https://github.com/pdfme/pdfme).

It allows you to generate PDF documents in Java using the same JSON-based template approach used by pdfme, without requiring Node.js, JavaScript, or a browser runtime.

Built on top of **Apache PDFBox**, pdfme4j is designed to make pdfme-style PDF generation available to Java applications, backend services, Spring Boot applications, CLI tools, and other JVM-based projects.

---

## Features

* Pure Java implementation
* No Node.js dependency
* No JavaScript runtime required
* Generate PDFs from pdfme-compatible JSON templates
* Apache PDFBox based
* Custom font registration
* Base PDF support
* Multiple data records
* Extensible plugin architecture
* Custom plugin registration
* Barcode and QR code generation
* SVG rendering
* Tables
* Shapes
* Images
* Text and multi-variable text
* Signature/image support

---

## Supported Plugins

The following plugins are currently registered by default:

| Type                | Plugin                    | Description                               |
| ------------------- | ------------------------- | ----------------------------------------- |
| `text`              | `TextPlugin`              | Render text content                       |
| `multiVariableText` | `MultiVariableTextPlugin` | Render text containing multiple variables |
| `image`             | `ImagePlugin`             | Render images                             |
| `signature`         | `ImagePlugin`             | Signature/image rendering                 |
| `line`              | `ShapePlugin`             | Draw lines                                |
| `rectangle`         | `ShapePlugin`             | Draw rectangles                           |
| `circle`            | `ShapePlugin`             | Draw circles                              |
| `qrcode`            | `BarcodePlugin`           | Generate QR codes                         |
| `barcode`           | `BarcodePlugin`           | Generate barcodes                         |
| `barcodes`          | `BarcodePlugin`           | Barcode rendering                         |
| `table`             | `TablePlugin`             | Render tables                             |
| `svg`               | `SvgPlugin`               | Render SVG content                        |

The plugin system is extensible, so applications can register their own plugins.

---

## Requirements

* Java 17+
* Maven 3.8+ recommended

---

## Installation

### Maven

The artifact is published under:

```xml
<groupId>io.github.sevketbuyukdemir</groupId>
<artifactId>pdfme4j</artifactId>
```

Add pdfme4j to your Maven project using the latest released version:

```xml
<dependency>
    <groupId>io.github.sevketbuyukdemir</groupId>
    <artifactId>pdfme4j</artifactId>
    <version>${pdfme4j.version}</version>
</dependency>
```

For example, define the version in your Maven properties:

```xml
<properties>
    <pdfme4j.version>0.1.0</pdfme4j.version>
</properties>
```

The library brings its required PDFBox, Jackson, ZXing, Batik, and related dependencies transitively.

---

## Basic Usage

A PDF can be generated directly from a pdfme JSON template.

```java
import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static void main(String[] args) throws Exception {

        String templateJson = Files.readString(
                Path.of("template.json")
        );

        Generator generator = new Generator();

        List<Map<String, String>> inputs = List.of(
                new HashMap<>()
        );

        byte[] pdf = generator.generate(
                templateJson,
                inputs
        );

        Files.write(
                Path.of("output.pdf"),
                pdf
        );
    }
}
```

---

## Using Template Data

Templates can be populated using input data.

For example:

```java
Map<String, String> data = new HashMap<>();

data.put("name", "John Doe");
data.put("company", "Example Inc.");
data.put("invoiceNumber", "INV-2026-001");

List<Map<String, String>> inputs = List.of(data);

byte[] pdf = generator.generate(
        templateJson,
        inputs
);
```

This makes it possible to reuse the same template for multiple documents.

For example:

```java
List<Map<String, String>> inputs = List.of(
        Map.of(
                "name", "John Doe",
                "invoiceNumber", "INV-001"
        ),
        Map.of(
                "name", "Jane Doe",
                "invoiceNumber", "INV-002"
        )
);

byte[] pdf = generator.generate(
        templateJson,
        inputs
);
```

---

## Generating from a `Template`

If you already have a `Template` object, you can pass it directly to the generator:

```java
Template template = ...;

byte[] pdf = generator.generate(
        template,
        inputs
);
```

This overload is useful when templates are already deserialized or generated programmatically.

---

## Custom Fonts

Custom fonts can be registered with the generator.

```java
Generator generator = new Generator();

generator.registerFont(
        "Arial",
        new File("/path/to/arial.ttf")
);
```

The registered font can then be referenced by the template's font configuration.

Multiple fonts can be registered:

```java
generator.registerFont(
        "Arial",
        new File("/fonts/arial.ttf")
);

generator.registerFont(
        "Roboto",
        new File("/fonts/Roboto-Regular.ttf")
);

generator.registerFont(
        "Roboto-Bold",
        new File("/fonts/Roboto-Bold.ttf")
);
```

You can also access the registered custom fonts:

```java
Map<String, File> fonts = generator.getCustomFonts();
```

---

## Base PDF

pdfme4j supports using an existing PDF as the base document.

A base PDF can be supplied through the template configuration as supported by pdfme's template structure.

The generator loads the existing PDF and renders the template elements on top of it.

This is useful for:

* PDF forms
* invoices
* certificates
* official documents
* pre-designed documents
* letterheads
* existing PDF templates

---

## Plugin Architecture

pdfme4j uses a plugin-based rendering architecture.

Plugins are associated with a schema type:

```java
plugins.put("text", new TextPlugin());
plugins.put("image", new ImagePlugin());
plugins.put("table", new TablePlugin());
```

Applications can register custom plugins using:

```java
generator.registerPlugin(
        "myPlugin",
        new MyPlugin()
);
```

A custom plugin must implement:

```java
IPdfmePlugin
```

This allows pdfme4j to be extended without modifying the core generator.

---

## Example Custom Plugin

A custom plugin can be registered like this:

```java
public class MyPlugin implements IPdfmePlugin {

    @Override
    public void render(
            PDDocument document,
            PDPageContentStream contentStream,
            SchemaItem item,
            String value,
            float pageHeightPoints
    ) throws Exception {

        // Custom rendering logic
    }
}
```

Then register it:

```java
Generator generator = new Generator();

generator.registerPlugin(
        "myPlugin",
        new MyPlugin()
);
```

Your template can then use:

```json
{
  "type": "myPlugin",
  "content": "Hello"
}
```

---

## PDF Generation Flow

The basic rendering flow is:

```text
pdfme JSON Template
        │
        ▼
     Template
        │
        ▼
     Generator
        │
        ├── TextPlugin
        ├── ImagePlugin
        ├── TablePlugin
        ├── BarcodePlugin
        ├── ShapePlugin
        ├── SvgPlugin
        └── Custom Plugins
        │
        ▼
   Apache PDFBox
        │
        ▼
     PDF bytes
```

The resulting PDF is returned as a `byte[]`, allowing the application to decide what to do with it.

For example:

```java
byte[] pdf = generator.generate(templateJson, inputs);
```

You can then:

* save it to disk
* return it from a REST endpoint
* upload it to object storage
* attach it to an email
* store it in a database
* stream it to another service

---

## Spring Boot Example

pdfme4j can be used directly from a Spring Boot application.

For example:

```java
@RestController
public class PdfController {

    private final Generator generator = new Generator();

    @PostMapping(
            value = "/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public byte[] generatePdf(
            @RequestBody String templateJson
    ) throws Exception {

        List<Map<String, String>> inputs = List.of(
                new HashMap<>()
        );

        return generator.generate(
                templateJson,
                inputs
        );
    }
}
```

This allows pdfme4j to be used as a server-side PDF generation engine.

---

## Testing

The project includes template-based tests under:

```text
src/test/resources/templates
```

Every JSON template in that directory can be loaded and rendered automatically.

The test suite:

1. Finds all JSON templates
2. Creates a `Generator`
3. Registers custom fonts when available
4. Generates a PDF for every template
5. Verifies that PDF data was generated
6. Writes generated PDFs into:

```text
target/
```

For example:

```text
target/
├── invoice_target.pdf
├── certificate_target.pdf
├── table_target.pdf
└── ...
```

Run the tests with:

```bash
mvn test
```

---

## Project Structure

A simplified project structure:

```text
pdfme4j/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── io/github/sevketbuyukdemir/pdfme4j/
│   │           ├── generator/
│   │           │   └── Generator.java
│   │           ├── model/
│   │           └── plugin/
│   │               ├── BarcodePlugin.java
│   │               ├── ImagePlugin.java
│   │               ├── MultiVariableTextPlugin.java
│   │               ├── ShapePlugin.java
│   │               ├── SvgPlugin.java
│   │               ├── TablePlugin.java
│   │               ├── TextPlugin.java
│   │               └── ...
│   │
│   └── test/
│       ├── java/
│       │   └── ...
│       └── resources/
│           └── templates/
│               ├── ...
│               └── *.json
│
├── pom.xml
└── README.md
```

---

## Dependencies

pdfme4j is built using established open-source Java libraries, including:

* [Apache PDFBox](https://pdfbox.apache.org/)
* [Jackson](https://github.com/FasterXML/jackson)
* [ZXing](https://github.com/zxing/zxing)
* [Apache Batik](https://xmlgraphics.apache.org/batik/)
* [Graphics2D for PDFBox](https://github.com/rototor/pdfbox-graphics2d)
* [JUnit 5](https://junit.org/junit5/) for testing

These dependencies are managed by Maven.

---

## Why pdfme4j?

[pdfme](https://github.com/pdfme/pdfme) provides a powerful JSON-based PDF generation model.

However, Java applications may not always have a Node.js runtime available or desirable.

pdfme4j aims to provide a native JVM solution while keeping the pdfme template concept familiar:

```text
pdfme template
      │
      ▼
   pdfme4j
      │
      ▼
Apache PDFBox
      │
      ▼
     PDF
```

This makes pdfme-style templates practical for Java backend applications without introducing a JavaScript runtime into the deployment.

---

## Compatibility

pdfme4j is intended to be compatible with the JSON template concepts used by pdfme.

Because pdfme is primarily a JavaScript/TypeScript project and pdfme4j is a native Java implementation, some rendering behavior may differ between the two implementations.

The project is continuously working toward broader template and rendering compatibility.

---

## Contributing

Contributions are welcome.

If you find a bug, have a feature request, or want to improve compatibility with pdfme:

1. Fork the repository
2. Create a feature branch
3. Add or update tests
4. Implement your changes
5. Run the test suite

```bash
mvn test
```

Then open a pull request.

When adding support for a new pdfme feature, adding a representative JSON template under:

```text
src/test/resources/templates
```

is strongly recommended.

---

## Roadmap

The project is actively evolving toward broader pdfme compatibility.

Potential areas of development include:

* Improved template compatibility
* Improved font handling
* More complete pdfme plugin compatibility
* Additional SVG features
* More advanced table rendering
* Improved text layout
* Additional barcode formats
* Better cross-platform font handling
* Additional automated compatibility tests

---

## License

pdfme4j is released under the **MIT License**.

See the `LICENSE` file for details.

---

## Author

**Şevket Büyükdemir**

* GitHub: [sevketbuyukdemir](https://github.com/sevketbuyukdemir)
* Project: [pdfme4j](https://github.com/sevketbuyukdemir/pdfme4j)

---

## Related Project

pdfme4j is a Java port inspired by:

**pdfme**

https://github.com/pdfme/pdfme

The goal of this project is to bring the pdfme template and plugin concepts to the Java ecosystem.
