package com.wavve.arsxml2wv.diagram;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.StringUtil;
import com.wavve.arsxml2wv.common.Tsv;

/**
 * Diagram XML 파싱과 검토용 목록 생성.
 * 좌표·스타일 등 화면 요소는 읽지 않는다. 요소 텍스트는 직접 들어 있는 문자(CDATA 포함)만 읽고,
 * 같은 이름의 요소가 여러 개면 마지막 값을 쓴다 (Go encoding/xml 동작과 동일).
 */
public final class DiagramService {
    private DiagramService() {
    }

    /** Diagram XML 을 읽는다. DOCTYPE·외부 엔티티는 거부한다(XXE 방지). */
    public static Diagram parse(InputStream in) throws AppException {
        Document doc = read(in);
        String enc = doc.getXmlEncoding();
        if (enc != null && !enc.equalsIgnoreCase("utf-8")) {
            throw new AppException("parse XML: encoding \"" + enc + "\" is not supported; save the XML as UTF-8");
        }
        Element root = doc.getDocumentElement();
        if (!"Diagram".equals(XmlUtil.localName(root))) {
            throw new AppException("parse XML: expected element type <Diagram> but have <" + XmlUtil.localName(root) + ">");
        }
        return Diagram.create(toNodes(root), toLinks(root));
    }

    private static Document read(InputStream in) throws AppException {
        try {
            DocumentBuilder builder = secureFactory().newDocumentBuilder();
            builder.setErrorHandler(THROW_ALL);
            return builder.parse(in);
        } catch (SAXParseException e) {
            throw new AppException("parse XML: " + describe(e), e);
        } catch (SAXException e) {
            throw new AppException("parse XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new AppException("parse XML: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new AppException("parse XML: " + e.getMessage(), e);
        }
    }

    /** 파서 메시지는 OS 언어로 나오므로 위치(줄·열)를 붙이고, DOCTYPE 거부는 고정 문구로 바꾼다. */
    private static String describe(SAXParseException e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains(DISALLOW_DOCTYPE)) {
            msg = "DOCTYPE is not allowed";
        }
        return "line " + e.getLineNumber() + ", column " + e.getColumnNumber() + ": " + msg;
    }

    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private static DocumentBuilderFactory secureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature(DISALLOW_DOCTYPE, true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f;
    }

    /** 파서 경고를 콘솔에 찍지 않고 오류는 예외로 올린다. */
    private static final ErrorHandler THROW_ALL = new ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    };

    private static List<Node> toNodes(Element root) {
        List<Node> nodes = new ArrayList<Node>();
        for (Element group : XmlUtil.children(root, "Nodes")) {
            for (Element x : XmlUtil.children(group, "Node")) {
                Map<String, String> props = new LinkedHashMap<String, String>();
                for (Element cp : XmlUtil.children(x, "CustomProperties")) {
                    for (Element p : XmlUtil.children(cp, null)) {
                        props.put(XmlUtil.localName(p), StringUtil.trimSpace(XmlUtil.directText(p)));
                    }
                }
                String text = StringUtil.trimSpace(XmlUtil.lastChildText(x, "Text"));
                nodes.add(new Node(x.getAttribute("Id"), x.getAttribute("NodeType"), text, props));
            }
        }
        return nodes;
    }

    private static List<Link> toLinks(Element root) {
        List<Link> links = new ArrayList<Link>();
        for (Element group : XmlUtil.children(root, "Links")) {
            for (Element x : XmlUtil.children(group, "Link")) {
                String from = XmlUtil.lastChildAttr(x, "Origin", "Id");
                String to = XmlUtil.lastChildAttr(x, "Destination", "Id");
                String text = StringUtil.trimSpace(XmlUtil.lastChildText(x, "Text"));
                links.add(new Link(x.getAttribute("Id"), from, to, text));
            }
        }
        return links;
    }

    /** 검토용 노드 목록. */
    public static String nodesTsv(Diagram d) {
        Tsv t = new Tsv("node_id", "node_type", "node_name", "target_page", "comment");
        for (Node n : d.nodes()) {
            t.row(n.id(), n.type(), n.text(), n.prop("TargetPage"), n.prop("Comment"));
        }
        return t.toString();
    }

    /** 검토용 링크 목록. */
    public static String linksTsv(Diagram d) {
        Tsv t = new Tsv("link_id", "origin_id", "origin_name", "branch_text", "destination_id", "destination_name");
        for (Link l : d.links()) {
            t.row(l.id(), l.from(), d.node(l.from()).text(), l.text(), l.to(), d.node(l.to()).text());
        }
        return t.toString();
    }
}
