package com.wavve.arsxml2wv.wv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.StringUtil;

/** 노드 1개에 대응하는 WV 화면. records 는 JS 문자열 안에 그대로 들어갈 레코드다. */
public final class Screen {
    // 레코드 구분자는 JS 소스 기준 \$ (한글 Windows 표기 ₩$), 레코드 끝은 ; 이다.
    private static final String FIELD_SEP = "\\$";

    private final String nodeId;
    private final String nodeName;
    private final String code;
    private final List<String> records;

    private Screen(String nodeId, String nodeName, String code, List<String> records) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.code = code;
        this.records = Collections.unmodifiableList(records);
    }

    public String nodeId() {
        return nodeId;
    }

    public String nodeName() {
        return nodeName;
    }

    public String code() {
        return code;
    }

    public List<String> records() {
        return records;
    }

    /** 완료 화면만 A, 나머지는 B 다. */
    public String readTimeout() {
        return ScreenCode.COMPLETE.equals(code) ? "A" : "B";
    }

    private static String backButton(String code) {
        return ScreenCode.NUMBER_INPUT.equals(code) ? "ON" : "OFF";
    }

    /** 공통 레코드(S, BTH, TIT, TXT) 뒤에 본문 레코드를 붙인다. */
    static Screen create(String id, String name, String code, String title, String text, List<String[]> body)
            throws AppException {
        List<String[]> rows = new ArrayList<String[]>(Arrays.asList(
                new String[] {"S", code},
                new String[] {"BTH", "0", backButton(code)},
                new String[] {"BTH", "1", "ON"},
                new String[] {"TIT", "0", title},
                new String[] {"TXT", "0", "C", text}));
        rows.addAll(body);
        List<String> records = new ArrayList<String>();
        for (String[] r : rows) {
            records.add(record(r[0], Arrays.copyOfRange(r, 1, r.length)));
        }
        return new Screen(id, StringUtil.oneLine(name), code, records);
    }

    /** TYPE\$f1\$f2; 형태를 만든다. 구분자($, ;)가 들어간 값은 규격을 깨므로 거부한다. */
    static String record(String type, String... fields) throws AppException {
        StringBuilder b = new StringBuilder(type);
        for (String f : fields) {
            if (f.indexOf('$') >= 0 || f.indexOf(';') >= 0) {
                throw new AppException("protocol delimiter ($ or ;) in text " + StringUtil.goQuote(f));
            }
            b.append(FIELD_SEP).append(jsEscape(f));
        }
        return b.append(';').toString();
    }

    private static String jsEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r\n", " ").replace("\n", " ")
                .replace("\r", " ").replace(" ", " ").replace(" ", " ");
    }

    /** 출력 파일 이름. 순번 접두어로 정렬과 충돌 방지를 함께 한다. */
    public String fileName(int seq) {
        String safeId = nodeId.replaceAll("[^A-Za-z0-9_-]", "_");
        return String.format(Locale.ROOT, "%03d_node_%s_%s.js", seq, safeId, code);
    }
}
