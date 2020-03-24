package ru.citeck.ecos.process.aop.eapps.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.eapps.casetemplate.ModuleChangesListener;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TrackChangesAspect<T> {

    private final ModuleChangesListener<T> changesListener;

    @Pointcut("@annotation(ru.citeck.ecos.process.aop.eapps.listener.TrackChanges)")
    public void servicePointcut() {
    }

    @AfterReturning(pointcut = "servicePointcut()", returning = "catchedObject")
    public void trackAfterReturn(JoinPoint joinPoint, T catchedObject) {
        log.info("Performed '" + joinPoint.getSignature().getName() + "' on object: " + catchedObject.toString());
        changesListener.perform(catchedObject);
    }

}
