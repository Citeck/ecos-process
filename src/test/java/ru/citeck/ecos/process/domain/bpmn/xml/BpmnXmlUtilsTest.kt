package ru.citeck.ecos.process.domain.bpmn.xml

// import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
// import kotlin.test.Test

// TODO: check after java 17 migration
// class BpmnXmlUtilsTest {
//
//    @Test
//    fun `bpmn xml with many attributes should read without error`() {
//
//        BpmnXmlUtils.readFromString(
//            """
//                <?xml version="1.0" encoding="UTF-8"?>
//                <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:xs="http://www.w3.org/2001/XMLSchema" id="Definitions_lvOBsjv" name="Процесс выдачи лицензии ECOS" targetNamespace="http://bpmn.io/schema/bpmn" exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="8.2.0" ecos:workingCopySourceRef="" ecos:name_ml="{&#34;ru&#34;:&#34;Процесс выдачи лицензии ECOS&#34;}" ecos:formRef="" ecos:sectionRef="" ecos:enabled="false" ecos:autoStartEnabled="true" ecos:defState="RAW" ecos:processDefId="ecos-license-request-process" ecos:ecosType="emodel/type@ecos-license-request">
//                  <bpmn:process id="ecos-license-request-process" isExecutable="true">
//                    <bpmn:sendTask id="Activity_0tnazdf" name="Уведомление о начале выдачи лицензии" ecos:notificationCc="[]" ecos:notificationBcc="[]" ecos:notificationLang="ru" ecos:notificationFrom="" ecos:asyncConfig="{&#34;asyncBefore&#34;:false,&#34;asyncAfter&#34;:false}" ecos:name_ml="{&#34;ru&#34;:&#34;Уведомление о начале выдачи лицензии&#34;}" ecos:notificationCcExpression="[&#34;&#34;]" ecos:number="" ecos:notificationTemplate="notifications/template@wf-ecos-license-before-issuing" ecos:notificationBccExpression="[&#34;&#34;]" ecos:notificationAdditionalMeta="{}" ecos:notificationTo="[&#34;issuers&#34;]"   ecos:documentation="" ecos:notificationRecord="" ecos:notificationTitle="" ecos:jobConfig="{}" ecos:notificationType="EMAIL_NOTIFICATION" ecos:notificationBody="" ecos:notificationToExpression="[&#34;&#34;]"></bpmn:sendTask>
//                    <bpmn:sendTask id="Activity_0dahjse" name="1"                                    ecos:notificationCc="[]" ecos:notificationBcc="[]" ecos:notificationLang="ru" ecos:notificationFrom="" ecos:asyncConfig="{&#34;asyncBefore&#34;:false,&#34;asyncAfter&#34;:false}" ecos:name_ml="{&#34;ru&#34;:&#34;1&#34;}"                                    ecos:notificationCcExpression="[&#34;&#34;]" ecos:number="" ecos:notificationTemplate="notifications/template@wf-ecos-license-before-rework"  ecos:notificationBccExpression="[&#34;&#34;]" ecos:notificationAdditionalMeta="{}" ecos:notificationTo="[&#34;initiator&#34;]" ecos:documentation="" ecos:notificationRecord="" ecos:notificationTitle="" ecos:jobConfig="{}" ecos:notificationType="EMAIL_NOTIFICATION" ecos:notificationBody="" ecos:notificationToExpression="[&#34;&#34;]"></bpmn:sendTask>
//                  </bpmn:process>
//                </bpmn:definitions>
//            """.trimIndent()
//        )
//    }
// }
