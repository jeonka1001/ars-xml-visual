package com.wavve.arsxml2wv.label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wavve.arsxml2wv.common.GoRegex;
import com.wavve.arsxml2wv.common.StringUtil;
import com.wavve.arsxml2wv.common.Tsv;
import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.diagram.Link;
import com.wavve.arsxml2wv.diagram.Node;
import com.wavve.arsxml2wv.input.Spec;

/** 메뉴 노드의 키별 버튼 문구를 정한다. */
public final class LabelService {
    private LabelService() {
    }

    /** 메뉴 노드의 키별 문구를 정한다. */
    public static LabelResult resolve(Diagram d, Spec s, Map<String, String> overrides) {
        Map<String, String> byOverride = new HashMap<String, String>();
        for (String k : s.keys()) {
            byOverride.put(k, overrides.get(s.nodeId() + ".button." + k));
        }
        Map<String, String> byComment = fromComment(s.guide());
        Map<String, String> byBranch = fromBranches(d, s.nodeId());
        List<Button> buttons = new ArrayList<Button>();
        for (String k : s.keys()) {
            buttons.add(Button.pick(k, byOverride, byComment, byBranch));
        }
        return new LabelResult(s.nodeId(), buttons);
    }

    // 주석 문형 예: "지정가는 1번", "수정하시려면 2번", "다시듣고싶으시면 별표", "이전단계로 가시려면 우물정자".
    private static final String S = GoRegex.S;
    private static final Pattern KEY_PHRASE = GoRegex.compile("^(.+?)" + S + "*([0-9])" + S + "*번|^(.+?)" + S
            + "*(별표|\\*|우물" + S + "*정자|우물정|샵|샾|#)");
    private static final Pattern PART_SEP = GoRegex.compile("[,.]");
    // 긴 어미부터 지운다. 남는 문구가 어색할 수 있으므로 결과는 항상 review 대상이다.
    private static final List<String> ENDINGS = Arrays.asList("을 원하시면", "를 원하시면", "돌아가시려면", "가시려면",
            "고 싶으시면", "고싶으시면", "하시려면", "으시려면", "시려면", "려면", "으시면", "으면", "시면", "은", "는");

    /** 안내 주석에서 키별 문구를 뽑는다. 같은 키에 다른 문구가 나오면 그 키는 버린다. */
    public static Map<String, String> fromComment(String guide) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        Set<String> conflict = new HashSet<String>();
        for (String part : PART_SEP.split(guide, -1)) {
            String[] kt = splitKeyPhrase(StringUtil.trimSpace(part));
            if (kt == null || conflict.contains(kt[0])) {
                continue;
            }
            String old = out.get(kt[0]);
            if (old != null && !old.equals(kt[1])) {
                out.remove(kt[0]);
                conflict.add(kt[0]);
                continue;
            }
            out.put(kt[0], kt[1]);
        }
        return out;
    }

    /** {키, 문구} 를 반환한다. 키 안내가 없으면 null 이다. */
    private static String[] splitKeyPhrase(String part) {
        Matcher m = KEY_PHRASE.matcher(part);
        if (!m.find()) {
            return null;
        }
        if (m.group(2) != null) {
            return new String[] {m.group(2), trimEnding(m.group(1))};
        }
        String key = "별표*".contains(m.group(4)) ? "*" : "#";
        return new String[] {key, trimEnding(m.group(3))};
    }

    /** 어미를 지운다. 남는 말이 한 글자 이하면 원문을 쓴다(예: "맞으시면"). */
    static String trimEnding(String s) {
        s = StringUtil.trimSpace(s);
        for (String e : ENDINGS) {
            if (!s.endsWith(e)) {
                continue;
            }
            String t = StringUtil.trimSpace(s.substring(0, s.length() - e.length()));
            return t.codePointCount(0, t.length()) > 1 ? t : s;
        }
        return s;
    }

    // 이동 목적지를 나타내는 노드만 문구로 쓴다. TTS·조건 노드 이름은 버튼 문구로 부적절하다.
    private static final Set<String> BRANCH_TARGET =
            new HashSet<String>(Arrays.asList("GotoPageNode", "ReturnPageNode", "EmptyNode"));

    /** 메뉴 노드의 정상 입력("0") 링크를 따라가 입력체크 Switch 의 분기별 도착 노드 이름을 반환한다. */
    public static Map<String, String> fromBranches(Diagram d, String nodeId) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        String sw = findInputSwitch(d, nodeId);
        if (sw == null) {
            return out;
        }
        for (Link l : d.linksFrom(sw)) {
            Node to = d.node(l.to());
            if (BRANCH_TARGET.contains(to.type()) && StringUtil.nz(out.get(l.text())).isEmpty()) {
                out.put(l.text(), StringUtil.oneLine(to.text()));
            }
        }
        return out;
    }

    /** "0" 링크에서 시작해 Script/Empty 노드를 최대 2개 거쳐 app.inputDTMF Switch 를 찾는다. 없으면 null. */
    private static String findInputSwitch(Diagram d, String nodeId) {
        String cur = "";
        for (Link l : d.linksFrom(nodeId)) {
            if ("0".equals(l.text())) {
                cur = l.to();
            }
        }
        for (int hop = 0; !cur.isEmpty() && hop < 3; hop++) {
            Node n = d.node(cur);
            if ("SwitchNode".equals(n.type()) && n.prop("Condition").contains("inputDTMF")) {
                return cur;
            }
            if (!"ScriptNode".equals(n.type()) && !"EmptyNode".equals(n.type())) {
                return null;
            }
            cur = nextOf(d, cur);
        }
        return null;
    }

    /** 나가는 링크가 정확히 1개일 때 그 도착 노드. 아니면 빈 문자열. */
    private static String nextOf(Diagram d, String id) {
        List<Link> links = d.linksFrom(id);
        return links.size() == 1 ? links.get(0).to() : "";
    }

    /** labels.properties(UTF-8) 내용을 읽는다. 키의 \# \! \= \: \\ 이스케이프를 지원한다. */
    public static Map<String, String> parseProperties(String content) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (String line : content.split("\n", -1)) {
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (line.startsWith("﻿")) {
                line = line.substring(1);
            }
            line = StringUtil.trimSpace(line);
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
                continue;
            }
            String[] kv = splitProperty(line);
            out.put(kv[0], kv[1]);
        }
        return out;
    }

    private static String[] splitProperty(String line) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                b.append(line.charAt(++i));
                continue;
            }
            if (c == '=' || c == ':') {
                return new String[] {StringUtil.trimSpace(b.toString()), StringUtil.trimSpace(line.substring(i + 1))};
            }
            b.append(c);
        }
        return new String[] {StringUtil.trimSpace(b.toString()), ""};
    }

    /** 검토용 버튼 문구 목록. */
    public static String buttonsTsv(List<LabelResult> results) {
        Tsv t = new Tsv("node_id", "key", "label", "source");
        for (LabelResult r : results) {
            for (Button b : r.buttons()) {
                t.row(r.nodeId(), b.key(), b.label(), b.source().label());
            }
        }
        return t.toString();
    }
}
