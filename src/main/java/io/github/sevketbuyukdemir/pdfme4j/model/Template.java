package io.github.sevketbuyukdemir.pdfme4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Template {
    private Object basePdf;
    private List<List<Map<String, Object>>> schemas;

    public Template() {
    }

    public Object getBasePdf() {
        return basePdf;
    }

    public void setBasePdf(Object basePdf) {
        this.basePdf = basePdf;
    }

    public List<List<Map<String, Object>>> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<List<Map<String, Object>>> schemas) {
        this.schemas = schemas;
    }
}
