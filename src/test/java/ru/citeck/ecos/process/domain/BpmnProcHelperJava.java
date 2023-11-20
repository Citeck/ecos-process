package ru.citeck.ecos.process.domain;

import java.nio.charset.StandardCharsets;

public class BpmnProcHelperJava {

    public static byte[] buildProcDefXml(String id) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "            <bpmn:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "        xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
            "        xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n" +
            "        xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n" +
            "        id=\"Definitions_" + id + "\" targetNamespace=\"http://bpmn.io/schema/bpmn\"\n" +
            "        xmlns:ecos=\"http://www.citeck.ru/ecos/bpmn/1.0\"\n" +
            "        exporter=\"bpmn-js (https://demo.bpmn.io)\" exporterVersion=\"8.2.0\"\n" +
            "        ecos:processDefId=\"" + id + "\">\n" +
            "              <bpmn:process id=\"Process_" + id + "\" isExecutable=\"false\">\n" +
            "                <bpmn:startEvent id=\"StartEvent_0lly8qf\">\n" +
            "                  <bpmn:outgoing>Flow_15brz3r</bpmn:outgoing>\n" +
            "                </bpmn:startEvent>\n" +
            "                <bpmn:sequenceFlow id=\"Flow_15brz3r\" sourceRef=\"StartEvent_0lly8qf\" targetRef=\"Event_0fitnzy\"/>\n" +
            "                <bpmn:endEvent id=\"Event_0fitnzy\">\n" +
            "                  <bpmn:incoming>Flow_15brz3r</bpmn:incoming>\n" +
            "                </bpmn:endEvent>\n" +
            "              </bpmn:process>\n" +
            "            </bpmn:definitions>").getBytes(StandardCharsets.UTF_8);
    }
}
