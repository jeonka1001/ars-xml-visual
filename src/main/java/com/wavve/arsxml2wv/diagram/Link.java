package com.wavve.arsxml2wv.diagram;

/** 노드 간 연결. text 는 분기 조건 값(예: "1", "default")이다. */
public final class Link {
    private final String id;
    private final String from;
    private final String to;
    private final String text;

    public Link(String id, String from, String to, String text) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.text = text;
    }

    public String id() {
        return id;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String text() {
        return text;
    }
}
