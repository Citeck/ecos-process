package emtypes.process.cmmn

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.utils.FileUtils

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

        byte[] data = file.readAsBytes()

        try {

            Module module = new Module()
            module.setId(getId(file, data))
            module.setXmlData(data)
            return module
//           parse content -> get moduleId param
//           or generate 'PATH_FROM_TEMPLATES/filename' without extension

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Module data reading error. File: " + file.getPath(), e)
            throw new RuntimeException(e)
        }
    }

    private String getId(EcosFile file, byte[] data) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder()
        Document document = docBuilder.parse(new ByteArrayInputStream(data))

        String moduleId = document.getElementsByTagName("cmmn:case")["ns8:moduleId"];
        if (moduleId == null ) {
            String filepath = file.path.toString();
            String filename = file.name;
            String formattedFilepath = filepath.substring(filepath.indexOf("case/templates")+1);
            String formattedFilename = filename.substring(0, filepath.indexOf("."));
            return formattedFilepath + "/" + formattedFilename;
        }
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        String name = FileUtils.getValidName(module.getId()) + ".xml"

        root.createFile(name, (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(module.getXmlData())
        })
    }

    static class Module {
        String id
        byte[] xmlData
    }
}
