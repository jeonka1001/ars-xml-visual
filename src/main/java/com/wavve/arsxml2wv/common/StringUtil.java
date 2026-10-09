package com.wavve.arsxml2wv.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 문자열 공통 함수. 공백 판정은 Go unicode.IsSpace 와 같다
 * (Java trim()/Character.isWhitespace 와 기준이 달라 출력이 달라지는 것을 막는다).
 */
public final class StringUtil {
    private StringUtil() {
    }

    /** Go unicode.IsSpace 와 같은 공백 판정. */
    public static boolean isSpace(int cp) {
        switch (cp) {
            case '\t': case '\n': case 0x0B: case '\f': case '\r': case ' ':
            case 0x85: case 0xA0: case 0x1680: case 0x2028: case 0x2029:
            case 0x202F: case 0x205F: case 0x3000:
                return true;
            default:
                return cp >= 0x2000 && cp <= 0x200A;
        }
    }

    /** 앞뒤 공백 제거 (Go strings.TrimSpace). */
    public static String trimSpace(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isSpace(s.codePointAt(start))) {
            start += Character.charCount(s.codePointAt(start));
        }
        while (end > start && isSpace(s.codePointBefore(end))) {
            end -= Character.charCount(s.codePointBefore(end));
        }
        return s.substring(start, end);
    }

    /** 공백으로 나눈 단어 목록 (Go strings.Fields). */
    public static List<String> fields(String s) {
        List<String> out = new ArrayList<String>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < s.length(); i += Character.charCount(s.codePointAt(i))) {
            int cp = s.codePointAt(i);
            if (!isSpace(cp)) {
                word.appendCodePoint(cp);
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            out.add(word.toString());
        }
        return out;
    }

    /** 연속 공백·줄바꿈을 공백 하나로 합친다 (Go strings.Join(strings.Fields(s), " ")). */
    public static String oneLine(String s) {
        return join(fields(s), " ");
    }

    public static String join(List<String> parts, String sep) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                b.append(sep);
            }
            b.append(parts.get(i));
        }
        return b.toString();
    }

    /** null 을 빈 문자열로 바꾼다. */
    public static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Go strconv.Quote(%q) 와 같은 따옴표 표기. 오류 메시지를 Go 버전과 같게 유지한다. */
    public static String goQuote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i += Character.charCount(s.codePointAt(i))) {
            appendQuoted(b, s.codePointAt(i));
        }
        return b.append('"').toString();
    }

    private static void appendQuoted(StringBuilder b, int cp) {
        switch (cp) {
            case 0x07: b.append("\\a"); return;
            case '\b': b.append("\\b"); return;
            case '\f': b.append("\\f"); return;
            case '\n': b.append("\\n"); return;
            case '\r': b.append("\\r"); return;
            case '\t': b.append("\\t"); return;
            case 0x0B: b.append("\\v"); return;
            case '\\': b.append("\\\\"); return;
            case '"': b.append("\\\""); return;
            default:
                break;
        }
        if (isPrint(cp)) {
            b.appendCodePoint(cp);
        } else if (cp < 0x80) {
            b.append(String.format("\\x%02x", cp));
        } else if (cp <= 0xFFFF) {
            b.append(String.format("\\u%04x", cp));
        } else {
            b.append(String.format("\\U%08x", cp));
        }
    }

    /** Go unicode.IsPrint: 문자·기호·숫자·구두점과 ASCII 공백만 출력 가능으로 본다. */
    private static boolean isPrint(int cp) {
        if (cp == ' ') {
            return true;
        }
        switch (Character.getType(cp)) {
            case Character.CONTROL: case Character.FORMAT: case Character.PRIVATE_USE:
            case Character.SURROGATE: case Character.UNASSIGNED: case Character.SPACE_SEPARATOR:
            case Character.LINE_SEPARATOR: case Character.PARAGRAPH_SEPARATOR:
                return false;
            default:
                return true;
        }
    }
}
