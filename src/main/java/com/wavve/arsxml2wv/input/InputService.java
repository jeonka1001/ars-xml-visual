package com.wavve.arsxml2wv.input;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.GoRegex;
import com.wavve.arsxml2wv.common.StringUtil;
import com.wavve.arsxml2wv.common.Tsv;
import com.wavve.arsxml2wv.diagram.Node;

/**
 * PreScript 는 실행하지 않고 문자열로만 해석한다.
 * 단순 리터럴 대입 1회만 인정하고, 그 외(변수, 중복 대입, 분기문)는 오류로 돌려 사람이 확인하게 한다.
 */
public final class InputService {
    private InputService() {
    }

    private static final String S = GoRegex.S;
    private static final Pattern CONTROL_FLOW =
            GoRegex.compile(GoRegex.WORD_START + "(if|else|switch|for|while|function)" + GoRegex.WORD_END);
    private static final Pattern MENT_CODE = GoRegex.compileMultiline("^" + S + "*app\\.mentFileName" + S + "*=" + S
            + "*app\\.mentFolder" + S + "*\\+" + S + "*\"([A-Za-z0-9_]+)\"" + S + "*;?" + S + "*$");

    /** digitMask 를 설정하는 CallPageNode 인지 확인한다. */
    public static boolean isInputNode(Node n) {
        return "CallPageNode".equals(n.type()) && n.prop("PreScript").contains("digitMask");
    }

    /** 입력 노드의 PreScript 를 Spec 으로 해석한다. */
    public static Spec fromNode(Node n) throws AppException {
        String raw = n.prop("PreScript").replace("\r\n", "\n");
        String code = stripComments(raw);
        if (CONTROL_FLOW.matcher(code).find()) {
            throw new AppException("control flow in PreScript");
        }
        Spec s = new Spec();
        s.nodeId = n.id();
        s.nodeName = n.text();
        s.page = n.prop("TargetPage");
        s.guide = leadingComment(raw);
        readSettings(code, s);
        s.kind = Spec.kindOf(s.page, s.length);
        Matcher m = MENT_CODE.matcher(code);
        if (m.find()) {
            s.mentCode = m.group(1);
        }
        return s;
    }

    private static void readSettings(String code, Spec s) throws AppException {
        s.mask = literal(code, "digitMask");
        if (!Spec.validMask(s.mask)) {
            throw new AppException("invalid digitMask " + StringUtil.goQuote(s.mask));
        }
        s.termMask = literal(code, "termDigitMask");
        s.length = digitLength(code);
    }

    /** digitLength 또는 운영 XML 의 오타 digitLegth 중 하나만 허용한다. */
    static int digitLength(String code) throws AppException {
        String a = null;
        String b = null;
        List<String> errors = new ArrayList<String>();
        try {
            a = literal(code, "digitLength");
        } catch (AppException e) {
            errors.add(e.getMessage());
        }
        try {
            b = literal(code, "digitLegth");
        } catch (AppException e) {
            errors.add(e.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new AppException(StringUtil.join(errors, "\n")); // Go errors.Join 과 같은 표기
        }
        if (!a.isEmpty() && !b.isEmpty()) {
            throw new AppException("both digitLength and digitLegth assigned");
        }
        return parseLength(a + b);
    }

    /** Go strconv.Atoi 와 같이 ASCII 숫자(부호 허용)만 받는다. */
    private static int parseLength(String v) throws AppException {
        try {
            int n = v.matches("[+-]?[0-9]+") ? Integer.parseInt(v) : 0;
            if (n >= 1) {
                return n;
            }
        } catch (NumberFormatException e) {
            // 범위 초과는 아래 오류로 처리
        }
        throw new AppException("invalid digit length " + StringUtil.goQuote(v));
    }

    /** app.&lt;name&gt; = "값"; 형태의 단독 대입 값을 반환한다. 대입이 없으면 빈 문자열이다. */
    static String literal(String code, String name) throws AppException {
        String q = Pattern.quote(name);
        Matcher all = GoRegex.compile(GoRegex.WORD_START + "app" + S + "*\\." + S + "*" + q + S + "*=[^=]").matcher(code);
        int count = 0;
        while (all.find()) {
            count++;
        }
        if (count == 0) {
            return "";
        }
        if (count > 1) {
            throw new AppException(name + " assigned " + count + " times");
        }
        Matcher m = GoRegex.compileMultiline("^" + S + "*app" + S + "*\\." + S + "*" + q + S + "*=" + S
                + "*\"([^\"\\\\\\n]*)\"" + S + "*;?" + S + "*$").matcher(code);
        if (!m.find()) {
            throw new AppException(name + " is not a simple string literal");
        }
        return m.group(1);
    }

    /** PreScript 맨 앞의 연속된 // 주석을 한 줄로 합친다. */
    static String leadingComment(String raw) {
        List<String> parts = new ArrayList<String>();
        for (String line : raw.split("\n", -1)) {
            line = StringUtil.trimSpace(line);
            if (line.isEmpty() && parts.isEmpty()) {
                continue;
            }
            if (!line.startsWith("//")) {
                break;
            }
            parts.add(StringUtil.trimSpace(line.substring(2)));
        }
        return StringUtil.join(parts, " ");
    }

    /** 문자열 리터럴 밖의 주석을 지운다. 줄 단위 매칭을 위해 줄바꿈은 남긴다. */
    static String stripComments(String s) throws AppException {
        StringBuilder b = new StringBuilder();
        int[] rs = s.codePoints().toArray();
        int quote = 0;
        for (int i = 0; i < rs.length; i++) {
            int c = rs[i];
            if (quote != 0) {
                b.appendCodePoint(c);
                if (c == '\\' && i + 1 < rs.length) {
                    b.appendCodePoint(rs[++i]);
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
                b.appendCodePoint(c);
            } else if (c == '/' && i + 1 < rs.length && (rs[i + 1] == '/' || rs[i + 1] == '*')) {
                i = skipComment(rs, i, b);
            } else {
                b.appendCodePoint(c);
            }
        }
        return b.toString();
    }

    /** rs[i] 에서 시작하는 주석을 건너뛰고 마지막 위치를 반환한다. */
    private static int skipComment(int[] rs, int i, StringBuilder b) throws AppException {
        if (rs[i + 1] == '/') {
            while (i < rs.length && rs[i] != '\n') {
                i++;
            }
            b.append('\n');
            return i;
        }
        for (int j = i + 2; j + 1 < rs.length; j++) {
            if (rs[j] == '*' && rs[j + 1] == '/') {
                for (int k = i; k < j; k++) {
                    if (rs[k] == '\n') {
                        b.append('\n');
                    }
                }
                return j + 1;
            }
        }
        throw new AppException("unclosed block comment in PreScript");
    }

    /** 검토용 입력 노드 목록. */
    public static String specsTsv(List<Spec> specs) {
        Tsv t = new Tsv("node_id", "node_name", "kind", "page", "mask", "length", "term_mask", "ment_code", "guide");
        for (Spec s : specs) {
            t.row(s.nodeId, s.nodeName, s.kind.label(), s.page, s.mask, String.valueOf(s.length), s.termMask, s.mentCode, s.guide);
        }
        return t.toString();
    }
}
