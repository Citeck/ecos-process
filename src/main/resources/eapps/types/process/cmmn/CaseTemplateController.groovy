package eapps.types.process.cmmn

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.citeck.ecos.apps.artifact.ArtifactMeta
import ru.citeck.ecos.apps.artifact.controller.ArtifactController
import ru.citeck.ecos.commons.io.file.EcosFile

import java.util.function.Function
import java.util.stream.Collectors

class CmmnControllerImpl implements ArtifactController<Artifact, Unit> {

    private static final Logger log = LoggerFactory.getLogger(CmmnControllerImpl.class)

    @Override
    List<Artifact> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**.xml")
            .stream()
            .map(new Function<EcosFile, Artifact>() {
                @Override
                Artifact apply(EcosFile module) {
                    return readModule(root, module)
                }
            })
            .collect(Collectors.toList())
    }

    private static Artifact readModule(EcosFile root, EcosFile file) {

        String path = root.getPath()
            .relativize(file.getPath())
            .toString()
            .replace("\\", "/")

        return new Artifact(path, file.readAsBytes())
    }

    @Override
    void write(@NotNull EcosFile root, Artifact module, Unit config) {

        root.createFile(module.getFilePath(), (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(module.getData())
        })
    }

    @Override
    ArtifactMeta getMeta(@NotNull Artifact artifact, @NotNull Unit unit) {
        return ArtifactMeta.create()
            .withId(artifact.getFilePath())
            .build()
    }
}

class Artifact {

    String filePath
    byte[] data

    Artifact(String path, byte[] data) {
        this.filePath = path
        this.data = data
    }

    Artifact() {}
}

return new CmmnControllerImpl()
