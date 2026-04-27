package com.instantaccessprovider.arcconsolidator.model;

import java.util.List;

/**
 * Modèle Scope (périmètre métier optionnel pour une Assignment)
 */
public class Scope {
    private List<Dimension> dimensions;

    public static class Dimension {
        private String type;
        private Object value;
        private String label;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public List<Dimension> getDimensions() { return dimensions; }
    public void setDimensions(List<Dimension> dimensions) { this.dimensions = dimensions; }
}
