package com.wavve.arsxml2wv.common;

import java.util.regex.Pattern;

/**
 * Go(RE2) 정규식과 같은 결과를 내기 위한 보조 상수.
 * Java 의 \s 는 \v 를 포함하고, \b 는 한글도 단어 문자로 보며, $ 는 \r·  도 줄 끝으로 본다.
 * Go 와 다르므로 아래 상수로 명시하고 UNIX_LINES(\n 만 줄 끝)로 컴파일한다.
 */
public final class GoRegex {
    private GoRegex() {
    }

    /** Go \s */
    public static final String S = "[\\t\\n\\f\\r ]";
    /** Go \b 중 단어 시작 경계 (ASCII 단어 문자 기준) */
    public static final String WORD_START = "(?<![0-9A-Za-z_])";
    /** Go \b 중 단어 끝 경계 (ASCII 단어 문자 기준) */
    public static final String WORD_END = "(?![0-9A-Za-z_])";

    public static Pattern compile(String regex) {
        return Pattern.compile(regex, Pattern.UNIX_LINES);
    }

    /** (?m) 다중 행 모드. ^ $ 는 \n 기준이다. */
    public static Pattern compileMultiline(String regex) {
        return Pattern.compile(regex, Pattern.UNIX_LINES | Pattern.MULTILINE);
    }
}
