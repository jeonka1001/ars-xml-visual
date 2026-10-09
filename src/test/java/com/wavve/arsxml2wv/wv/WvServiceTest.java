package com.wavve.arsxml2wv.wv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.diagram.DiagramService;
import com.wavve.arsxml2wv.input.Kind;
import com.wavve.arsxml2wv.input.Spec;
import com.wavve.arsxml2wv.label.Button;
import com.wavve.arsxml2wv.label.LabelResult;
import com.wavve.arsxml2wv.label.Source;

public class WvServiceTest {
    static final String FLOW_XML = "<Diagram><Nodes>\n"
            + "<Node Id=\"1\" NodeType=\"MemoNode\"><Text>1-2-3 잔고 조회</Text></Node>\n"
            + "<Node Id=\"2\" NodeType=\"CallPageNode\"><Text>조회</Text><CustomProperties><PreScript>app.trCode = \"abc100q01\";</PreScript></CustomProperties></Node>\n"
            + "<Node Id=\"3\" NodeType=\"CallPageNode\"><Text>확인 메뉴</Text></Node>\n"
            + "<Node Id=\"4\" NodeType=\"CallPageNode\"><Text>처리</Text><CustomProperties><PreScript>app.trCode = \"abc200u01\";</PreScript></CustomProperties></Node>\n"
            + "<Node Id=\"5\" NodeType=\"ScriptNode\"><Text>결과</Text></Node>\n"
            + "<Node Id=\"6\" NodeType=\"CallPageNode\"><Text>완료 메뉴</Text></Node>\n"
            + "<Node Id=\"7\" NodeType=\"CallPageNode\"><Text>종목</Text><CustomProperties><TargetPage>sh_menu3_jmcode.xml</TargetPage></CustomProperties></Node>\n"
            + "</Nodes><Links>\n"
            + "<Link Id=\"10\"><Origin Id=\"2\"/><Destination Id=\"3\"/></Link>\n"
            + "<Link Id=\"11\"><Origin Id=\"3\"/><Destination Id=\"4\"/></Link>\n"
            + "<Link Id=\"12\"><Origin Id=\"4\"/><Destination Id=\"5\"/></Link>\n"
            + "<Link Id=\"13\"><Origin Id=\"5\"/><Destination Id=\"6\"/></Link>\n"
            + "</Links></Diagram>";

    static WvInput testInput(Map<String, String> overrides) throws Exception {
        Diagram d = DiagramService.parse(new ByteArrayInputStream(FLOW_XML.getBytes(StandardCharsets.UTF_8)));
        List<Spec> specs = Arrays.asList(
                new Spec("3", "확인 메뉴", Kind.MENU, "12*", 1, "", "맞으시면 1번, 취소는 2번"),
                new Spec("6", "완료 메뉴", Kind.MENU, "1", 1, "", "내역을 확인하십시오. 처음으로는 1번"),
                new Spec("2", "금액\n입력", Kind.NUMBER, "0123456789", 8, "", "금액을 입력하십시오. 예시입니다."));
        List<LabelResult> buttons = Arrays.asList(
                new LabelResult("3", Arrays.asList(new Button("1", "확인", Source.OVERRIDE),
                        new Button("2", "취소", Source.COMMENT), new Button("*", "", Source.NONE))),
                new LabelResult("6", Arrays.asList(new Button("1", "처음으로", Source.COMMENT))));
        return new WvInput(d, specs, buttons, overrides);
    }

    static Screen find(List<Screen> screens, String id) {
        for (Screen s : screens) {
            if (s.nodeId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    @Test
    public void buildAll() throws Exception {
        List<Screen> screens = WvController.buildAll(testInput(new HashMap<String, String>()), new ArrayList<String>());
        assertEquals(4, screens.size());
        assertEquals(ScreenCode.NUMBER_INPUT, find(screens, "2").code());
        assertEquals(ScreenCode.CONFIRM, find(screens, "3").code());
        assertEquals(ScreenCode.COMPLETE, find(screens, "6").code());
        assertEquals(ScreenCode.STOCK_SEARCH, find(screens, "7").code());
    }

    @Test
    public void renderMenu() throws Exception {
        Map<String, String> o = new HashMap<String, String>();
        o.put("3.text", "안내 \"따옴표\"");
        String js = WvService.render(find(WvController.buildAll(testInput(o), new ArrayList<String>()), "3"));
        for (String want : new String[] {
            "wvMenu += \"S\\$SHKD12;\";",
            "wvMenu += \"TIT\\$0\\$잔고 조회;\";",
            "wvMenu += \"TXT\\$0\\$C\\$안내 \\\"따옴표\\\";\";",
            "wvMenu += \"BTN\\$0\\$확인\\$1;\";",
            "wvMenu += \"BTN\\$1\\$취소\\$2;\";",
            "app.wvReadTimeout = \"B\";",
        }) {
            assertTrue("missing " + want + " in\n" + js, js.contains(want));
        }
        assertFalse("unresolved key must not become a button", js.contains("\\$*;"));
    }

    @Test
    public void numberScreen() throws Exception {
        Map<String, String> o = new HashMap<String, String>();
        o.put("title", "공통 제목");
        WvInput in = testInput(o);
        Screen s = WvService.numberScreen(in, in.specs.get(2), new ArrayList<String>());
        assertEquals(Arrays.asList("S\\$SHKC21;", "BTH\\$0\\$ON;", "BTH\\$1\\$ON;", "TIT\\$0\\$공통 제목;",
                "TXT\\$0\\$C\\$금액을 입력하십시오.;", "INPH\\$0\\$8\\$8\\$금액 입력\\$N\\$ON;", "INBTN\\$0\\$확인;"), s.records());
    }

    @Test
    public void screenOverrideAndDelimiterReject() throws Exception {
        Map<String, String> o = new HashMap<String, String>();
        o.put("6.screen", "SHKX99");
        o.put("3.text", "a;b");
        List<String> notes = new ArrayList<String>();
        List<Screen> screens = WvController.buildAll(testInput(o), notes);
        assertEquals(null, find(screens, "3"));
        assertEquals("SHKX99", find(screens, "6").code());
        assertTrue(notes.toString(), notes.contains("node 3: screen skipped: protocol delimiter ($ or ;) in text \"a;b\""));
    }

    @Test
    public void recordEscapesBackslash() throws Exception {
        assertEquals("TXT\\$a\\\\b\\$줄 바꿈;", Screen.record("TXT", "a\\b", "줄\n바꿈"));
        try {
            Screen.record("TXT", "a$b");
            fail("expected delimiter error");
        } catch (AppException expected) {
            // ok
        }
    }

    @Test
    public void renderDoesNotReplaceInsideValues() throws Exception {
        Map<String, String> o = new HashMap<String, String>();
        o.put("title", "{{records}}");
        String js = WvService.render(find(WvController.buildAll(testInput(o), new ArrayList<String>()), "3"));
        assertTrue(js, js.contains("TIT\\$0\\${{records}};"));
    }
}
