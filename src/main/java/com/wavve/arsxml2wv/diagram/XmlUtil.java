package com.wavve.arsxml2wv.diagram;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

/** DOM 탐색 보조 함수. Go encoding/xml 처럼 접두어 없는 이름으로 비교한다. */
final class XmlUtil {
    private XmlUtil() {
    }

    static String localName(Element e) {
        String name = e.getTagName();
        int i = name.indexOf(':');
        return i < 0 ? name : name.substring(i + 1);
    }

    /** 직계 자식 요소 목록. name 이 null 이면 전체. */
    static List<Element> children(Element e, String name) {
        List<Element> out = new ArrayList<Element>();
        for (org.w3c.dom.Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && (name == null || name.equals(localName((Element) n)))) {
                out.add((Element) n);
            }
        }
        return out;
    }

    /** 요소에 직접 들어 있는 텍스트와 CDATA 만 합친다. 하위 요소·주석은 제외한다. */
    static String directText(Element e) {
        StringBuilder b = new StringBuilder();
        for (org.w3c.dom.Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            short t = n.getNodeType();
            if (t == org.w3c.dom.Node.TEXT_NODE || t == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                b.append(n.getNodeValue());
            }
        }
        return b.toString();
    }

    /** 같은 이름의 자식이 여러 개면 마지막 것의 텍스트. 없으면 빈 문자열. */
    static String lastChildText(Element e, String name) {
        List<Element> c = children(e, name);
        return c.isEmpty() ? "" : directText(c.get(c.size() - 1));
    }

    /** 같은 이름의 자식이 여러 개면 마지막 것의 속성 값. 없으면 빈 문자열. */
    static String lastChildAttr(Element e, String name, String attr) {
        List<Element> c = children(e, name);
        return c.isEmpty() ? "" : c.get(c.size() - 1).getAttribute(attr);
    }
}
