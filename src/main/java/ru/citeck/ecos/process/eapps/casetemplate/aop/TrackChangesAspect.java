package ru.citeck.ecos.process.eapps.casetemplate.aop;

import io.github.jhipster.config.JHipsterConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.eapps.casetemplate.CaseTemplateModuleHandler;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TrackChangesAspect {

    private final CaseTemplateModuleHandler caseTemplateModuleHandler;

    @Pointcut("@annotation(TrackChanges)")
    public void servicePointcut() {
    }

    @AfterReturning(pointcut = "servicePointcut()", returning = "entity")
    public void trackAfterReturn(JoinPoint joinPoint, CaseTemplateEntity entity) {
        log.info("Performed '" + joinPoint.getSignature().getName() + "' on case template with id: " + entity.getId());
        caseTemplateModuleHandler.perform(entity);
    }

}
