package com.wavve.arsxml2wv.label;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.diagram.DiagramService;
import com.wavve.arsxml2wv.input.Kind;
import com.wavve.arsxml2wv.input.Spec;

public class LabelServiceTest {
    @Test
    public void fromComment() {
        String guide = "메뉴를 선택해 주십시오. 잔고조회는 1번, 이체를 원하시면 2번, 처음으로 돌아가시려면 9번, "
                + "다시 들으시려면 별표, 이전단계로 가시려면 우물정자를 눌러주십시오.";
        Map<String, String> want = new LinkedHashMap<String, String>();
        want.put("1", "잔고조회");
        want.put("2", "이체");
        want.put("9", "처음으로");
        want.put("*", "다시 들");
        want.put("#", "이전단계로");
        assertEquals(want, LabelService.fromComment(guide));
    }

    @Test
    public void fromCommentShortStemKeepsPhrase() {
        assertEquals("맞으시면", LabelService.fromComment("맞으시면 1번").get("1"));
    }

    @Test
    public void fromCommentConflictDropsKey() {
        Map<String, String> got = LabelService.fromComment("조회는 1번, 이체는 1번, 취소는 2번");
        assertFalse(got.containsKey("1"));
        assertEquals("취소", got.get("2"));
    }

    static final String BRANCH_XML = "<Diagram><Nodes>\n"
            + "<Node Id=\"1\" NodeType=\"CallPageNode\"><Text>메뉴</Text></Node>\n"
            + "<Node Id=\"2\" NodeType=\"ScriptNode\"><Text>세팅</Text></Node>\n"
            + "<Node Id=\"3\" NodeType=\"SwitchNode\"><Text>입력체크</Text><CustomProperties><Condition>app.inputDTMF</Condition></CustomProperties></Node>\n"
            + "<Node Id=\"4\" NodeType=\"GotoPageNode\"><Text>초기\n메뉴</Text></Node>\n"
            + "<Node Id=\"5\" NodeType=\"CallPageNode\"><Text>TTS 안내</Text></Node>\n"
            + "<Node Id=\"6\" NodeType=\"GotoPageNode\"><Text>오류</Text></Node>\n"
            + "</Nodes><Links>\n"
            + "<Link Id=\"10\"><Text>0</Text><Origin Id=\"1\"/><Destination Id=\"2\"/></Link>\n"
            + "<Link Id=\"11\"><Text>maxErr_noinput</Text><Origin Id=\"1\"/><Destination Id=\"6\"/></Link>\n"
            + "<Link Id=\"12\"><Text>ok</Text><Origin Id=\"2\"/><Destination Id=\"3\"/></Link>\n"
            + "<Link Id=\"13\"><Text>9</Text><Origin Id=\"3\"/><Destination Id=\"4\"/></Link>\n"
            + "<Link Id=\"14\"><Text>1</Text><Origin Id=\"3\"/><Destination Id=\"5\"/></Link>\n"
            + "</Links></Diagram>";

    static Diagram branchDiagram() throws Exception {
        return DiagramService.parse(new ByteArrayInputStream(BRANCH_XML.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void resolvePriority() throws Exception {
        Spec s = new Spec("1", "", Kind.MENU, "129#", 1, "", "조회는 1번, 이체는 2번");
        LabelResult got = LabelService.resolve(branchDiagram(), s, Collections.singletonMap("1.button.2", "계좌이체"));
        List<Button> b = got.buttons();
        assertEquals(4, b.size());
        assertButton(b.get(0), "1", "조회", Source.COMMENT);
        assertButton(b.get(1), "2", "계좌이체", Source.OVERRIDE);
        assertButton(b.get(2), "9", "초기 메뉴", Source.BRANCH);
        assertButton(b.get(3), "#", "", Source.NONE);
    }

    private static void assertButton(Button b, String key, String label, Source source) {
        assertEquals(Arrays.asList(key, label, source), Arrays.<Object>asList(b.key(), b.label(), b.source()));
    }

    @Test
    public void fromBranchesSkipsNonTargetNodes() throws Exception {
        Map<String, String> got = LabelService.fromBranches(branchDiagram(), "1");
        assertNull(got.get("1"));
        assertEquals("초기 메뉴", got.get("9"));
    }

    @Test
    public void parseProperties() {
        String src = "﻿# 주석\n! 주석\n31.title = 청약 안내\r\n31.button.\\#=이전 단계\n31.button.*: 다시 듣기\n\n";
        Map<String, String> want = new HashMap<String, String>();
        want.put("31.title", "청약 안내");
        want.put("31.button.#", "이전 단계");
        want.put("31.button.*", "다시 듣기");
        assertEquals(want, LabelService.parseProperties(src));
    }
}
