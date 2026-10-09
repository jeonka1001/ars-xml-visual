package com.wavve.arsxml2wv.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.diagram.Node;

public class InputServiceTest {
    static Node node(String page, String pre) {
        Map<String, String> props = new HashMap<String, String>();
        props.put("TargetPage", page);
        props.put("PreScript", pre);
        return new Node("7", "CallPageNode", "메뉴", props);
    }

    static final String MENU_SCRIPT = "\n\n"
            + "// 맞으시면 1번, 수정하시려면 2번을 눌러주십시오.\n"
            + "app.mentFileName = app.mentFolder + \"A361416\";\n"
            + "app.mentFileFormat = \"V\";\n\n"
            + "app.digitMask = \"12*#\";\n"
            + "app.digitLegth = \"1\";\n"
            + "app.termDigitMask = \"\";\n\n"
            + "// 오입력, 미입력 최대 오류횟수\n"
            + "app.nomatchMaxCnt = 5;\n"
            + "/* app.digitMask = \"9\"; */\n"
            + "app.InputDTMF_type = \"1\";";

    @Test
    public void fromNodeMenu() throws Exception {
        Spec s = InputService.fromNode(node("InputDTMF_Menu.xml", MENU_SCRIPT.replace("\n", "\r\n")));
        assertEquals("7", s.nodeId());
        assertEquals("메뉴", s.nodeName());
        assertEquals("InputDTMF_Menu.xml", s.page());
        assertEquals(Kind.MENU, s.kind());
        assertEquals("12*#", s.mask());
        assertEquals(1, s.length());
        assertEquals("", s.termMask());
        assertEquals("A361416", s.mentCode());
        assertEquals("맞으시면 1번, 수정하시려면 2번을 눌러주십시오.", s.guide());
    }

    @Test
    public void fromNodeNumber() throws Exception {
        String pre = "app.digitMask = \"1234567890\";\napp.digitLength = \"8\";\napp.termDigitMask = \"*#\";";
        Spec s = InputService.fromNode(node("inputDTMF2.xml", pre));
        assertEquals(Kind.NUMBER, s.kind());
        assertEquals(8, s.length());
        assertEquals("*#", s.termMask());
        assertEquals("", s.guide());
    }

    @Test
    public void fromNodeRejects() {
        Map<String, Node> cases = new LinkedHashMap<String, Node>();
        cases.put("dynamic mask", node("InputDTMF_Menu.xml", "app.digitMask = mask;\napp.digitLength = \"1\";"));
        cases.put("duplicate mask", node("InputDTMF_Menu.xml", "app.digitMask = \"1\";\napp.digitMask = \"2\";\napp.digitLength = \"1\";"));
        cases.put("both lengths", node("InputDTMF_Menu.xml", "app.digitMask = \"1\";\napp.digitLength = \"1\";\napp.digitLegth = \"1\";"));
        cases.put("missing length", node("InputDTMF_Menu.xml", "app.digitMask = \"1\";"));
        cases.put("bad mask char", node("InputDTMF_Menu.xml", "app.digitMask = \"1A\";\napp.digitLength = \"1\";"));
        cases.put("menu multi digit", node("InputDTMF_Menu.xml", "app.digitMask = \"12\";\napp.digitLength = \"2\";"));
        cases.put("unknown page", node("Other.xml", "app.digitMask = \"1\";\napp.digitLength = \"1\";"));
        cases.put("control flow", node("InputDTMF_Menu.xml", "if (x) { app.digitMask = \"1\"; }\napp.digitLength = \"1\";"));
        cases.put("unclosed comment", node("InputDTMF_Menu.xml", "/* app.digitMask = \"1\";"));
        cases.put("non-ascii digit", node("InputDTMF_Menu.xml", "app.digitMask = \"1\";\napp.digitLength = \"１\";"));
        for (Map.Entry<String, Node> c : cases.entrySet()) {
            try {
                InputService.fromNode(c.getValue());
                fail(c.getKey() + ": expected error");
            } catch (AppException expected) {
                // ok
            }
        }
    }

    /** Go 와 같은 오류 문구 (review.txt 가 바이트 단위로 같아야 함). */
    @Test
    public void errorMessagesMatchGo() {
        assertMessage("digitMask is not a simple string literal", node("InputDTMF_Menu.xml", "app.digitMask = mask;\napp.digitLength = \"1\";"));
        assertMessage("invalid digitMask \"1\\u00a0\"", node("InputDTMF_Menu.xml", "app.digitMask = \"1 \";\napp.digitLength = \"1\";"));
        assertMessage("digitLength assigned 2 times\ndigitLegth is not a simple string literal",
                node("InputDTMF_Menu.xml", "app.digitMask = \"1\";\napp.digitLength = \"1\";\napp.digitLength = \"2\";\napp.digitLegth = x;"));
        assertMessage("unsupported input page \"Other.xml\"", node("Other.xml", "app.digitMask = \"1\";\napp.digitLength = \"1\";"));
    }

    private static void assertMessage(String want, Node n) {
        try {
            InputService.fromNode(n);
            fail("expected error: " + want);
        } catch (AppException e) {
            assertEquals(want, e.getMessage());
        }
    }

    /** Go \b 는 ASCII 기준: 한글 바로 뒤의 if 도 분기문으로 본다. */
    @Test
    public void wordBoundaryIsAscii() {
        assertMessage("control flow in PreScript", node("InputDTMF_Menu.xml", "가if;\napp.digitMask = \"1\";\napp.digitLength = \"1\";"));
    }

    @Test
    public void stripCommentsKeepsStrings() throws Exception {
        String got = InputService.stripComments("a = \"http://x\"; // c\nb = '/*y*/';");
        assertEquals("a = \"http://x\"; \nb = '/*y*/';", got);
    }

    @Test
    public void keysDedup() {
        Spec s = new Spec("1", "", Kind.MENU, "1219*#*", 1, "", "");
        assertEquals(Arrays.asList("1", "2", "9", "*", "#"), s.keys());
    }

    @Test
    public void isInputNode() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("PreScript", "app.digitMask");
        assertFalse(InputService.isInputNode(new Node("1", "ScriptNode", "", props)));
    }
}
