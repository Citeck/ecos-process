#!/bin/sh

echo "The application will start in ${ECOS_INIT_DELAY:0}s..." && sleep ${ECOS_INIT_DELAY:0}
exec java ${JAVA_OPTS} \
-Decos.env.container \
-noverify \
-XX:+AlwaysPreTouch \
-Djava.security.egd=file:/dev/./urandom \
-cp /app/resources/:/app/classes/:/app/libs/* \
"ru.citeck.ecos.process.EprocApp" \
"$@"
