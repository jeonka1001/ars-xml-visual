package com.wavve.arsxml2wv.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wavve.arsxml2wv.common.AppException;

/** XML 1개에서 읽은 노드와 링크 목록. */
public final class Diagram {
    private final List<Node> nodes;
    private final List<Link> links = new ArrayList<Link>();
    private final List<String> warnings = new ArrayList<String>(); // 변환은 계속하되 사람이 확인할 항목
    private final Map<String, Node> index = new HashMap<String, Node>();

    private Diagram(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(new ArrayList<Node>(nodes));
    }

    /** 노드 Id 누락·중복을 거부하고, 끝점이 없는 링크는 경고로 남긴다. */
    static Diagram create(List<Node> nodes, List<Link> links) throws AppException {
        Diagram d = new Diagram(nodes);
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n.id().isEmpty()) {
                throw new AppException("node without Id (position " + i + ")");
            }
            if (d.index.put(n.id(), n) != null) {
                throw new AppException("duplicate node Id: " + n.id());
            }
        }
        for (Link l : links) {
            if (!d.has(l.from()) || !d.has(l.to())) {
                d.warnings.add("link " + l.id() + ": unresolved endpoint " + l.from() + " -> " + l.to());
                continue;
            }
            d.links.add(l);
        }
        return d;
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Link> links() {
        return Collections.unmodifiableList(links);
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean has(String id) {
        return index.containsKey(id);
    }

    /** Id 로 노드를 찾는다. 없으면 Node.EMPTY 다. */
    public Node node(String id) {
        Node n = index.get(id);
        return n == null ? Node.EMPTY : n;
    }

    /** 해당 노드에서 나가는 링크 목록. */
    public List<Link> linksFrom(String id) {
        List<Link> out = new ArrayList<Link>();
        for (Link l : links) {
            if (l.from().equals(id)) {
                out.add(l);
            }
        }
        return out;
    }
}
