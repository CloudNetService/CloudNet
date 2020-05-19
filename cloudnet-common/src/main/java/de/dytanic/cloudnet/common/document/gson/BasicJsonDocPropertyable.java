package de.dytanic.cloudnet.common.document.gson;

public class BasicJsonDocPropertyable implements IJsonDocPropertyable {

    protected JsonDocument properties = new JsonDocument();

    @Override
    public <E> IJsonDocPropertyable setProperty(JsonDocProperty<E> docProperty, E val) {
        this.properties.setProperty(docProperty, val);
        return this;
    }

    @Override
    public <E> E getProperty(JsonDocProperty<E> docProperty) {
        return this.properties.getProperty(docProperty);
    }

    @Override
    public <E> IJsonDocPropertyable removeProperty(JsonDocProperty<E> docProperty) {
        this.properties.removeProperty(docProperty);
        return this;
    }

    @Override
    public <E> boolean hasProperty(JsonDocProperty<E> docProperty) {
        return docProperty.tester.test(this.properties);
    }

    @Override
    public JsonDocument getProperties() {
        return this.properties;
    }
}