package com.wavve.arsxml2wv.wv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.GoRegex;
import com.wavve.arsxml2wv.common.StringUtil;
import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.diagram.Link;
import com.wavve.arsxml2wv.diagram.Node;
import com.wavve.arsxml2wv.input.Spec;
import com.wavve.arsxml2wv.label.Button;
import com.wavve.arsxml2wv.label.LabelService;

/** WV 화면을 만들고 JS 코드로 렌더링한다. 각 화면 함수는 notes 에 검토 항목을 먼저 남긴 뒤 오류를 던진다. */
public final class WvService {
    private WvService() {
    }

    static final String DEFAULT_TITLE = "보이는 ARS";
    static final String DEFAULT_MENU_TEXT = "원하시는 메뉴를 선택해 주세요.";
    static final String STOCK_SEARCH_TEXT = "종목명/종목코드 입력";
    private static final String TXT_NOTE = ": TXT taken from voice guide; review wording";

    private static final String TEMPLATE = loadTemplate();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private static String loadTemplate() {
        try (InputStream in = WvService.class.getResourceAsStream("/screen.js.tmpl")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0;) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("screen.js.tmpl not found in jar", e);
        }
    }

    /** 화면을 PreScript 에 붙여 넣을 JS 코드로 만든다. 치환은 한 번만 한다(값 안의 {{..}}는 그대로 둔다). */
    public static String render(Screen s) {
        StringBuilder records = new StringBuilder();
        for (String r : s.records()) {
            records.append("wvMenu += \"").append(r).append("\";\n");
        }
        Matcher m = PLACEHOLDER.matcher(TEMPLATE);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            String v;
            switch (m.group(1)) {
                case "nodeId": v = s.nodeId(); break;
                case "nodeName": v = s.nodeName(); break;
                case "records": v = records.toString(); break;
                case "readTimeout": v = s.readTimeout(); break;
                default: v = m.group();
            }
            m.appendReplacement(b, Matcher.quoteReplacement(v));
        }
        return m.appendTail(b).toString();
    }

    /** 메뉴 노드를 BTN 화면으로 만든다. 문구가 정해진 키만 버튼이 된다. */
    public static Screen menuScreen(WvInput in, Spec s, List<Button> buttons, Set<String> reach, List<String> notes)
            throws AppException {
        List<String[]> body = new ArrayList<String[]>();
        for (Button b : buttons) {
            if (b.resolved()) {
                body.add(new String[] {"BTN", String.valueOf(body.size()), b.label(), b.key()});
            }
        }
        if (body.isEmpty()) {
            throw new AppException("no resolved button");
        }
        String code = reach.contains(s.nodeId()) ? ScreenCode.COMPLETE : ScreenCode.CONFIRM;
        String text = menuText(in, s, notes);
        code = screenOverride(in, s.nodeId(), code, notes);
        if (ScreenCode.COMPLETE.equals(code)) {
            notes.add("node " + s.nodeId() + ": complete screen buttons follow the voice menu; check against service standard");
        }
        return Screen.create(s.nodeId(), s.nodeName(), code, titleOf(in, s.nodeId()), text, body);
    }

    /** 숫자 입력 노드를 INPH 화면으로 만든다. # 로 끝내는 가변 길이는 최소 1자리다. */
    public static Screen numberScreen(WvInput in, Spec s, List<String> notes) throws AppException {
        String minLen = s.termMask().contains("#") ? "1" : String.valueOf(s.length());
        String name = StringUtil.oneLine(s.nodeName());
        List<String[]> body = Arrays.asList(
                new String[] {"INPH", "0", minLen, String.valueOf(s.length()), name, "N", "ON"},
                new String[] {"INBTN", "0", "확인"});
        String text = in.override(s.nodeId() + ".text");
        if (text.isEmpty()) {
            text = firstSentence(s.guide(), name);
            notes.add("node " + s.nodeId() + TXT_NOTE);
        }
        String code = screenOverride(in, s.nodeId(), ScreenCode.NUMBER_INPUT, notes);
        return Screen.create(s.nodeId(), s.nodeName(), code, titleOf(in, s.nodeId()), text, body);
    }

    /** 공통 종목코드 입력 페이지 호출 노드를 종목 검색 화면으로 만든다. */
    public static Screen stockSearchScreen(WvInput in, Node n, List<String> notes) throws AppException {
        String title = titleOf(in, n.id());
        List<String[]> body = Arrays.asList(
                new String[] {"INPH", "0", "1", "8", STOCK_SEARCH_TEXT, "N", "ON"},
                new String[] {"INBTN", "0", "검색"},
                new String[] {"TXT", "1", "L", title + " 하실 종목을 검색해주세요."});
        String code = screenOverride(in, n.id(), ScreenCode.STOCK_SEARCH, notes);
        return Screen.create(n.id(), n.text(), code, title, STOCK_SEARCH_TEXT, body);
    }

    /** 공통 종목코드 입력 페이지(*_jmcode.xml)를 호출하는 노드인지 확인한다. */
    public static boolean isStockSearch(Node n) {
        return "CallPageNode".equals(n.type()) && n.prop("TargetPage").toLowerCase(Locale.ROOT).endsWith("_jmcode.xml");
    }

    private static String menuText(WvInput in, Spec s, List<String> notes) {
        String t = in.override(s.nodeId() + ".text");
        if (!t.isEmpty()) {
            return t;
        }
        t = firstSentence(s.guide(), "");
        if (!t.isEmpty() && LabelService.fromComment(t).isEmpty()) {
            notes.add("node " + s.nodeId() + TXT_NOTE);
            return t;
        }
        return DEFAULT_MENU_TEXT;
    }

    private static String screenOverride(WvInput in, String id, String code, List<String> notes) {
        String c = in.override(id + ".screen");
        if (!c.isEmpty()) {
            return c;
        }
        notes.add("node " + id + ": screen code " + code + " inferred");
        return code;
    }

    private static final Pattern MEMO_PREFIX = GoRegex.compile("^[0-9]+(-[0-9]+)*" + GoRegex.S + "*");

    /** &lt;id&gt;.title → title → 첫 MemoNode 이름(메뉴 번호 제거) → 기본값. */
    static String titleOf(WvInput in, String id) {
        for (String k : new String[] {id + ".title", "title"}) {
            if (!in.override(k).isEmpty()) {
                return in.override(k);
            }
        }
        for (Node n : in.diagram.nodes()) {
            if ("MemoNode".equals(n.type()) && !n.text().isEmpty()) {
                return MEMO_PREFIX.matcher(StringUtil.oneLine(n.text())).replaceFirst("");
            }
        }
        return DEFAULT_TITLE;
    }

    /** 안내 문구의 첫 문장(마침표 포함). 없으면 fallback 이다. */
    static String firstSentence(String guide, String fallback) {
        guide = StringUtil.trimSpace(guide);
        int i = guide.indexOf('.');
        if (i >= 0) {
            guide = guide.substring(0, i + 1);
        }
        return guide.isEmpty() ? fallback : guide;
    }

    private static final Pattern TR_CODE = GoRegex.compile("app\\.trCode" + GoRegex.S + "*=" + GoRegex.S + "*\"([A-Za-z0-9]+)\"");
    private static final Pattern UPDATE_TR = GoRegex.compile("u[0-9]+\\z");

    /**
     * 처리(update) 거래 노드 이후 링크로 도달하는 노드 집합.
     * trCode 가 u01 등으로 끝나면 처리 거래, q01·ARS012 등은 조회 거래로 본다.
     */
    public static Set<String> reachableAfterUpdate(Diagram d) {
        Set<String> seen = new HashSet<String>();
        Deque<String> queue = new ArrayDeque<String>();
        for (Node n : d.nodes()) {
            Matcher m = TR_CODE.matcher(n.prop("PreScript"));
            if (m.find() && UPDATE_TR.matcher(m.group(1)).find()) {
                queue.add(n.id());
            }
        }
        while (!queue.isEmpty()) {
            for (Link l : d.linksFrom(queue.poll())) {
                if (seen.add(l.to())) {
                    queue.add(l.to());
                }
            }
        }
        return seen;
    }
}
