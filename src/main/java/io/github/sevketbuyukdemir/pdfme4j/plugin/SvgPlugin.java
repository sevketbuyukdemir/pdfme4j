package io.github.sevketbuyukdemir.pdfme4j.plugin;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import io.github.sevketbuyukdemir.pdfme4j.generator.Generator;
import io.github.sevketbuyukdemir.pdfme4j.model.SchemaItem;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.w3c.dom.svg.SVGDocument;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;

public class SvgPlugin implements IPdfmePlugin {

    @Override
    public void render(PDDocument document, PDPageContentStream contentStream, SchemaItem item, String value, float pageHeightPoints) throws Exception {
        String svgContent = value != null ? value : item.getContent();
        if (svgContent == null || svgContent.isBlank()) {
            return;
        }
        float x = (float) item.getPosition().getX() * Generator.MM_TO_POINTS;
        float yMm = (float) item.getPosition().getY() * Generator.MM_TO_POINTS;
        float boxWidth = (float) item.getWidth() * Generator.MM_TO_POINTS;
        float boxHeight = (float) item.getHeight() * Generator.MM_TO_POINTS;
        float y = pageHeightPoints - yMm - boxHeight;

        if (boxWidth <= 0 || boxHeight <= 0) {
            return;
        }

        float opacity = item.getOpacity() != null ? item.getOpacity().floatValue() : 1.0f;
        opacity = Math.max(0f, Math.min(1f, opacity));
        contentStream.saveGraphicsState();

        try {
            if (opacity < 1f) {
                PDExtendedGraphicsState state = new PDExtendedGraphicsState();
                state.setNonStrokingAlphaConstant(opacity);
                state.setStrokingAlphaConstant(opacity);
                contentStream.setGraphicsStateParameters(state);
            }
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            SVGDocument svgDocument = factory.createSVGDocument(null, new StringReader(svgContent));
            UserAgentAdapter userAgent = new UserAgentAdapter();
            BridgeContext bridgeContext = new BridgeContext(userAgent);
            bridgeContext.setDynamicState(BridgeContext.STATIC);
            GVTBuilder builder = new GVTBuilder();
            GraphicsNode graphicsNode = builder.build(bridgeContext, svgDocument);
            if (graphicsNode == null) {
                throw new IllegalStateException("Batik returned null GraphicsNode");
            }
            Rectangle2D bounds = graphicsNode.getBounds();
            if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                throw new IllegalStateException("SVG has empty bounds: " + bounds);
            }
            double svgWidth = bounds.getWidth();
            double svgHeight = bounds.getHeight();
            double scaleX = boxWidth / svgWidth;
            double scaleY = boxHeight / svgHeight;
            double scale = Math.min(scaleX, scaleY);
            double renderedWidth = svgWidth * scale;
            double renderedHeight = svgHeight * scale;
            double offsetX = (boxWidth - renderedWidth) / 2.0;
            double offsetY = (boxHeight - renderedHeight) / 2.0;
            int formWidth = Math.max(1, (int) Math.ceil(boxWidth));
            int formHeight = Math.max(1, (int) Math.ceil(boxHeight));
            PdfBoxGraphics2D graphics = new PdfBoxGraphics2D(document, formWidth, formHeight);
            try {
                AffineTransform transform = new AffineTransform();
                transform.translate(offsetX, offsetY);
                transform.scale(scale, scale);
                transform.translate(-bounds.getX(), -bounds.getY());
                graphics.setTransform(transform);
                graphicsNode.paint(graphics);
            } finally {
                graphics.dispose();
            }
            contentStream.transform(Matrix.getTranslateInstance(x, y));
            contentStream.drawForm(graphics.getXFormObject());
        } catch (Exception e) {
            String name = item.getName() != null ? item.getName() : "<unnamed>";
            throw new IllegalStateException("SVG rendering failed for schema '" + name + "'", e);
        } finally {
            contentStream.restoreGraphicsState();
        }
    }
}
