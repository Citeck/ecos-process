package ru.citeck.ecos.process.domain.procdef.eapps.casetemplate;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.records2.RecordRef;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class CaseTemplateUtils {

    private static final String CMMN_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/MODEL";

    public static NewProcessDefDto parseCmmn(String filePath, byte[] data) {

        Node caseNode = getCaseNode(data);
        Map<String, String> attributes = getAttributes(caseNode);

        NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
        newProcessDefDto.setProcType("cmmn");
        newProcessDefDto.setData(data);
        newProcessDefDto.setEcosTypeRef(getCaseEcosType(attributes));
        newProcessDefDto.setFormat("xml");
        newProcessDefDto.setId(getModuleId(attributes, filePath));
        newProcessDefDto.setAlfType(attributes.get("caseType"));

        return newProcessDefDto;
    }

    private static String getModuleId(Map<String, String> attributes, String filePath) {
        String moduleId = attributes.get("moduleId");
        if (StringUtils.isBlank(moduleId)) {
            int lastDelim = filePath.lastIndexOf('/');
            if (lastDelim >= 0) {
                moduleId = filePath.substring(lastDelim + 1);
            } else {
                moduleId = filePath;
            }
        }
        return moduleId;
    }

    private static Map<String, String> getAttributes(Node caseNode) {

        Map<String, String> attributes = new HashMap<>();

        int length = caseNode.getAttributes().getLength();
        for (int i = 0; i < length; i++) {
            Node attNode = caseNode.getAttributes().item(i);
            String name = attNode.getNodeName();
            int delim = name.indexOf(':');
            if (delim > 0 && name.length() > delim + 1) {
                name = name.substring(delim + 1);
            }
            attributes.put(name, attNode.getNodeValue());
        }

        return attributes;
    }

    private static Node getCaseNode(byte[] data) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(new ByteArrayInputStream(data));
            NodeList nodeList = document.getElementsByTagNameNS(CMMN_NAMESPACE, "case");
            return nodeList.item(0);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static RecordRef getCaseEcosType(Map<String, String> attributes) {

        String type = attributes.get("caseEcosType");
        if (StringUtils.isNotBlank(type)) {
            type = type.replace("workspace-SpacesStore-", "");
        } else {
            return RecordRef.EMPTY;
        }
        String kind = attributes.get("caseEcosKind");
        if (StringUtils.isNotBlank(kind)) {
            kind = kind.replace("workspace-SpacesStore-", "");
        }

        String typeId = type + (StringUtils.isNotBlank(kind) ? "/" + kind : "");

        if (typeId.startsWith("emodel/type@")) {
            return RecordRef.valueOf(typeId);
        }

        return RecordRef.create("emodel", "type", typeId);
    }
}
