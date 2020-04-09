package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.process.dto.NewProcessDefDto;
import ru.citeck.ecos.process.service.ProcessDefService;
import ru.citeck.ecos.records2.RecordRef;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseTemplateModuleHandler implements EcosModuleHandler<CaseTemplateDto> {

    private static final String CASE_TEMPLATE_TYPE = "process/cmmn";

    private final ProcessDefService processService;

    @Override
    public void deployModule(@NotNull CaseTemplateDto dto) {

        log.info("Case template module received: " + dto.getFilePath());

        Node caseNode = getCaseNode(dto.getData());
        Map<String, String> attributes = getAttributes(caseNode);

        NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
        newProcessDefDto.setType("cmmn");
        newProcessDefDto.setData(dto.getData());
        newProcessDefDto.setEcosTypeRef(getCaseEcosType(attributes));
        newProcessDefDto.setFormat("xml");
        newProcessDefDto.setId(getModuleId(attributes, dto));
        newProcessDefDto.setAlfType(attributes.get("caseType"));

        processService.uploadProcDef(newProcessDefDto);
    }

    @NotNull
    @Override
    public ModuleWithMeta<CaseTemplateDto> getModuleMeta(@NotNull CaseTemplateDto dto) {

        Node caseNode = getCaseNode(dto.getData());
        Map<String, String> attributes = getAttributes(caseNode);

        String moduleId = getModuleId(attributes, dto);
        return new ModuleWithMeta<>(dto, new ModuleMeta(moduleId, Collections.emptyList()));
    }

    private String getModuleId(Map<String, String> attributes, CaseTemplateDto dto) {
        String moduleId = attributes.get("moduleId");
        if (StringUtils.isBlank(moduleId)) {
            moduleId = dto.getFilePath();
        }
        return moduleId;
    }

    private Map<String, String> getAttributes(Node caseNode) {

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

    private Node getCaseNode(byte[] data) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(new ByteArrayInputStream(data));
            NodeList nodeList = document.getElementsByTagName("cmmn:case");
            return nodeList.item(0);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private RecordRef getCaseEcosType(Map<String, String> attributes) {

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

        return RecordRef.create("emodel", "type", typeId);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return CASE_TEMPLATE_TYPE;
    }

    @Override
    public void listenChanges(@NotNull Consumer<CaseTemplateDto> consumer) {

    }

    @Nullable
    @Override
    public ModuleWithMeta<CaseTemplateDto> prepareToDeploy(@NotNull CaseTemplateDto caseTemplateDTO) {
        return getModuleMeta(caseTemplateDTO);
    }
}
