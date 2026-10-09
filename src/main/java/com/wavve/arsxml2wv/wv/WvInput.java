package com.wavve.arsxml2wv.wv;

import java.util.List;
import java.util.Map;

import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.input.Spec;
import com.wavve.arsxml2wv.label.LabelResult;

/** 화면 생성에 필요한 앞 단계 결과. */
public final class WvInput {
    final Diagram diagram;
    final List<Spec> specs;
    final List<LabelResult> buttons;
    final Map<String, String> overrides; // labels.properties: title, <id>.title, <id>.text, <id>.screen

    public WvInput(Diagram diagram, List<Spec> specs, List<LabelResult> buttons, Map<String, String> overrides) {
        this.diagram = diagram;
        this.specs = specs;
        this.buttons = buttons;
        this.overrides = overrides;
    }

    /** 설정값. 없으면 빈 문자열이다. */
    String override(String key) {
        String v = overrides.get(key);
        return v == null ? "" : v;
    }
}
