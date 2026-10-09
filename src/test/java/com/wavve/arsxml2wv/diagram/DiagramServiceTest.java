package com.wavve.arsxml2wv.diagram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.wavve.arsxml2wv.common.AppException;

public class DiagramServiceTest {
    static final String TEST_XML = "﻿<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<Diagram Version=\"14\">\n"
            + "  <Nodes>\n"
            + "    <Node Id=\"1\" NodeType=\"CallPageNode\">\n"
            + "      <Bounds>0, 0, 10, 10</Bounds>\n"
            + "      <Text>메뉴\n선택</Text>\n"
            + "      <CustomProperties>\n"
            + "        <PreScript><![CDATA[app.digitMask = \"12\";]]></PreScript>\n"
            + "        <TargetPage>InputDTMF_Menu.xml</TargetPage>\n"
            + "      </CustomProperties>\n"
            + "    </Node>\n"
            + "    <Node Id=\"2\" NodeType=\"SwitchNode\"><Text>입력체크</Text></Node>\n"
            + "  </Nodes>\n"
            + "  <Links>\n"
            + "    <Link Id=\"10\"><Text>ok</Text><Origin Id=\"1\" /><Destination Id=\"2\" /></Link>\n"
            + "    <Link Id=\"11\"><Text>1</Text><Origin Id=\"2\" /><Destination Id=\"99\" /></Link>\n"
            + "  </Links>\n"
            + "</Diagram>";

    static Diagram parse(String xml) throws AppException {
        return DiagramService.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void parse() throws Exception {
        Diagram d = parse(TEST_XML);
        Node n = d.node("1");
        assertEquals("CallPageNode", n.type());
        assertEquals("메뉴\n선택", n.text());
        assertEquals("app.digitMask = \"12\";", n.prop("PreScript"));
        assertEquals("InputDTMF_Menu.xml", n.prop("TargetPage"));
        assertEquals(1, d.links().size());
        assertEquals("ok", d.links().get(0).text());
        assertEquals(1, d.linksFrom("1").size());
        assertEquals(1, d.warnings().size());
    }

    @Test
    public void parseRejects() {
        Map<String, String> cases = new LinkedHashMap<String, String>();
        cases.put("duplicate id", "<Diagram><Nodes><Node Id=\"1\"/><Node Id=\"1\"/></Nodes></Diagram>");
        cases.put("missing id", "<Diagram><Nodes><Node NodeType=\"X\"/></Nodes></Diagram>");
        cases.put("wrong root", "<Other/>");
        cases.put("entity", "<!DOCTYPE d [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><Diagram><Nodes><Node Id=\"1\"><Text>&x;</Text></Node></Nodes></Diagram>");
        cases.put("non utf-8", "<?xml version=\"1.0\" encoding=\"EUC-KR\"?><Diagram/>");
        for (Map.Entry<String, String> c : cases.entrySet()) {
            try {
                parse(c.getValue());
                fail(c.getKey() + ": expected error");
            } catch (AppException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().length() > 0);
            }
        }
    }

    /** Go encoding/xml 과 같은 동작: 직접 텍스트만, 같은 이름은 마지막 값, 여러 Nodes 묶음 병합. */
    @Test
    public void parseMatchesGoEdgeCases() throws Exception {
        Diagram d = parse("<Diagram><Nodes><Node Id=\"1\"><Text> a<b>X</b>c<!--k--> </Text>"
                + "<CustomProperties><P>p1<i>z</i>p2<![CDATA[ q ]]></P><Q></Q><R><s>only</s></R></CustomProperties></Node></Nodes>"
                + "<Nodes><Node Id=\"2\"><Text>a</Text><Text>second</Text></Node></Nodes>"
                + "<Links><Link Id=\"9\"><Text>t1</Text><Text>t2</Text><Origin Id=\"1\"/><Origin Id=\"2\"/><Destination Id=\"2\"/></Link></Links></Diagram>");
        assertEquals("ac", d.node("1").text());
        assertEquals("p1p2 q", d.node("1").prop("P"));
        assertEquals("", d.node("1").prop("R"));
        assertEquals("second", d.node("2").text());
        assertEquals("2", d.links().get(0).from());
        assertEquals("t2", d.links().get(0).text());
    }

    @Test
    public void tsvEscapesCells() throws Exception {
        String got = DiagramService.nodesTsv(parse(TEST_XML));
        assertTrue(got, got.contains("1\tCallPageNode\t메뉴 선택\tInputDTMF_Menu.xml\t\n"));
    }
}
