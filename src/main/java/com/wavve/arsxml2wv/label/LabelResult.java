package com.wavve.arsxml2wv.label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 메뉴 노드 1개의 버튼 목록. 순서는 digitMask 순서를 따른다. */
public final class LabelResult {
    private final String nodeId;
    private final List<Button> buttons;

    public LabelResult(String nodeId, List<Button> buttons) {
        this.nodeId = nodeId;
        this.buttons = Collections.unmodifiableList(new ArrayList<Button>(buttons));
    }

    public String nodeId() {
        return nodeId;
    }

    public List<Button> buttons() {
        return buttons;
    }
}
