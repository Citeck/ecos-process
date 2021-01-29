package ru.citeck.ecos.process.domain.cmmn.io.xml;

import org.w3c.dom.ls.LSInput;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public abstract class XmlLsInput implements LSInput {

    private final byte[] xsdData;
    private final InputStream xsdDataStream;

    private String type;
    private String namespaceURI;
    private String publicId;
    private String systemId;
    private String baseURI;
    private Reader characterStream;
    private InputStream byteStream;
    private String encoding;
    private boolean certifiedText;

    public XmlLsInput(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        super();
        this.encoding = "UTF-8";
        this.type = type;
        this.namespaceURI = namespaceURI;
        this.publicId = publicId;
        this.systemId = systemId;
        this.baseURI = baseURI;

        this.xsdData = getXsdData(systemId);
        this.xsdDataStream = new ByteArrayInputStream(this.xsdData);
    }

    protected abstract byte[] getXsdData(String systemId);

    @Override
    public String getStringData() {
        try {
            return new String(this.xsdData, this.encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Cannot read schema", e);
        }
    }

    @Override
    public void setStringData(String stringData) {

    }

    public byte[] getXsdData() {
        return xsdData;
    }

    public InputStream getXsdDataStream() {
        return xsdDataStream;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public void setNamespaceURI(String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String getBaseURI() {
        return baseURI;
    }

    @Override
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    @Override
    public Reader getCharacterStream() {
        return characterStream;
    }

    @Override
    public void setCharacterStream(Reader characterStream) {
        this.characterStream = characterStream;
    }

    @Override
    public InputStream getByteStream() {
        return byteStream;
    }

    @Override
    public void setByteStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public boolean getCertifiedText() {
        return certifiedText;
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {
        this.certifiedText = certifiedText;
    }
}
