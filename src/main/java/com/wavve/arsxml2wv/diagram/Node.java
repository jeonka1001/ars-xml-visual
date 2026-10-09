package com.wavve.arsxml2wv.diagram;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.wavve.arsxml2wv.common.StringUtil;

/** 시나리오의 노드 1개. */
public final class Node {
    /** 찾는 노드가 없을 때 쓰는 빈 노드. */
    public static final Node EMPTY = new Node("", "", "", new LinkedHashMap<String, String>());

    private final String id;
    private final String type;
    private final String text;
    private final Map<String, String> props;

    /**
     * @param type  NodeType 속성 (예: CallPageNode)
     * @param text  디자이너에 표시되는 노드 이름
     * @param props CustomProperties 하위 요소 (예: PreScript, TargetPage)
     */
    public Node(String id, String type, String text, Map<String, String> props) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.props = Collections.unmodifiableMap(new LinkedHashMap<String, String>(props));
    }

    public String id() {
        return id;
    }

    public String type() {
        return type;
    }

    public String text() {
        return text;
    }

    /** CustomProperties 값을 반환한다. 없으면 빈 문자열이다. */
    public String prop(String name) {
        return StringUtil.nz(props.get(name));
    }
}
