package emtypes.process.cmmn

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.io.file.EcosFile

import java.util.function.Function
import java.util.stream.Collectors

class CaseTemplateController implements ModuleController<CaseTemplateController.Module, Unit> {

    private static final Logger log = LoggerFactory.getLogger(ModuleController.class)

    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**.xml")
            .stream()
            .map(new Function<EcosFile, Module>() {
                @Override
                Module apply(EcosFile module) {
                    return readModule(root, module)
                }
            })
            .collect(Collectors.toList())
    }

    private static Module readModule(EcosFile root, EcosFile file) {

        String path = root.getPath()
            .relativize(file.getPath())
            .toString()
            .replace("\\", "/")

        return new Module(path, file.readAsBytes())
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        root.createFile(module.getFilePath(), (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(module.getData())
        })
    }

    static class Module {

        String filePath
        byte[] data

        Module(String path, byte[] data) {
            this.filePath = path
            this.data = data
        }

        Module() {}
    }
}

return new CaseTemplateController()
