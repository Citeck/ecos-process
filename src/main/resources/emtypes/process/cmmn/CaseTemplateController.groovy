package emtypes.process.cmmn


import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.thymeleaf.util.StringUtils
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.utils.FileUtils
import ru.citeck.ecos.records2.RecordRef

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.util.stream.Collectors

return new ModuleController<Module, Unit>() {

    private static final Logger log = LoggerFactory.getLogger(ModuleController.class)

    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**.xml")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Module readModule(EcosFile file) {

        log.info("READ: " + file.name);
        byte[] data = file.readAsBytes()

        try {

            Module module = new Module()

            Node caseNode = getCaseNode(data)
//
            String moduleId = getModuleId(caseNode, file);
//            log.info("MODULE_ID: " + moduleId)
            module.setId(moduleId)
//            module.setId("123")
//
            RecordRef typeRef = getCaseEcosType(caseNode)
//            log.info("TYPE_REF: " + typeRef.toString())
            module.setTypeRef(typeRef);

//            module.setId(getId(file, data))
            module.setXmlContent(data)
            return module

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Module data reading error. File: " + file.getPath(), e)
            throw new RuntimeException(e)
        }
    }

    private Node getCaseNode(byte[] data) {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder()
        Document document = docBuilder.parse(new ByteArrayInputStream(data))
        NodeList nodeList = document.getElementsByTagName("cmmn:case");
        return nodeList.item(0);
    }

    private String getId(EcosFile file, byte[] data) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder()
        Document document = docBuilder.parse(new ByteArrayInputStream(data))
        NodeList nodeList = document.getElementsByTagName("cmmn:case");
        String moduleId = nodeList.item(0).getAttribute("ns8:moduleId").toString()

//        log.info(nodeList.item(0).hasAttributes().toString())
//        log.info(nodeList.item(0).getNodeName().toString())
//        log.info(nodeList.item(0).getAttribute("ns8:moduleId").toString())
        log.info(moduleId)

        if (moduleId != null && !moduleId.isEmpty() && file.path != null && file.name != null) {

            log.info("GET_ID_fp: " + file.path);
            log.info("GET_ID_fn: " + file.name);

            String filepath = file.path.toString();
            int prefix = filepath.indexOf("case/templates")

            if (file.name.isEmpty()) {
                throw new RuntimeException("Cannot receive module. Cannot get/generate name for it.")
            }

            if (prefix != -1) {
                String formattedFilepath = filepath.substring(prefix + 1);
                return formattedFilepath + "/" + filename;
            }
        }
        return file.name;
    }

    private String getModuleId(Node node, EcosFile file) {
        String moduleId = node.getAttribute("ns8:moduleId").toString()

        if (!StringUtils.isEmpty(moduleId) && file.path != null && file.name != null) {

            String filepath = file.path.toString();
            int prefix = filepath.indexOf("case/templates")

            if (file.name.isEmpty()) {
                throw new RuntimeException("Cannot receive module. Cannot get/generate name for it.")
            }

            if (prefix != -1) {
                String formattedFilepath = filepath.substring(prefix + 1)
                return formattedFilepath + "/" + file.name
            }
        }
        return file.name
    }

    private RecordRef getCaseEcosType(Node node) {

        String value =  node.getAttribute("ns8:caseEcosType").toString()
        if (!StringUtils.isEmpty(value)) {
            value = value.replaceAll("workspace-SpacesStore-", "")
            return RecordRef.create("emodel", "type", value)
        }
        return RecordRef.EMPTY;
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        log.info("WRITING: " + module.getId());
        String name = FileUtils.getValidName(module.getId())
        log.info("WRITING: " + name);

        root.createFile(name, (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(module.getXmlContent())
        })
    }

    static class Module {
        String id
        RecordRef typeRef
        byte[] xmlContent
    }
}
