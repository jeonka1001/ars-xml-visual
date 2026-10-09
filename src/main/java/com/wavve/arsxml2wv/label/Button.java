package com.wavve.arsxml2wv.label;

import java.util.Map;

/** 허용 키 1개의 문구. */
public final class Button {
    private final String key;
    private final String label;
    private final Source source;

    public Button(String key, String label, Source source) {
        this.key = key;
        this.label = label;
        this.source = source;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public Source source() {
        return source;
    }

    /** 키 문구가 정해졌는지. 정해지지 않은 키는 화면 버튼으로 만들지 않는다. */
    public boolean resolved() {
        return source != Source.NONE;
    }

    /** 사람이 문구를 다듬어야 하는지. */
    public boolean inferred() {
        return source == Source.COMMENT || source == Source.BRANCH;
    }

    /** 우선순위(설정 → 주석 → 분기) 순으로 첫 문구를 고른다. */
    @SafeVarargs
    static Button pick(String key, Map<String, String>... sources) {
        Source[] order = {Source.OVERRIDE, Source.COMMENT, Source.BRANCH};
        for (int i = 0; i < sources.length; i++) {
            String v = sources[i].get(key);
            if (v != null && !v.isEmpty()) {
                return new Button(key, v, order[i]);
            }
        }
        return new Button(key, "", Source.NONE);
    }
}
