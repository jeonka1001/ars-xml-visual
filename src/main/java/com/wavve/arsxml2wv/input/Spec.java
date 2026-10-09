package com.wavve.arsxml2wv.input;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.StringUtil;

/** 입력 노드 1개의 PreScript 설정값. */
public final class Spec {
    private static final String MENU_PAGE = "inputdtmf_menu.xml";

    String nodeId = "";
    String nodeName = "";
    String page = "";      // 호출 대상 페이지 (TargetPage)
    Kind kind;
    String mask = "";      // 허용 키 (digitMask)
    int length;            // 입력 자릿수 (digitLength, 운영 XML의 digitLegth 포함)
    String termMask = "";  // 종료 키 (termDigitMask)
    String mentCode = "";  // 안내 멘트 파일 코드 (예: A361121)
    String guide = "";     // PreScript 첫 주석의 안내 문구

    public Spec(String nodeId, String nodeName, Kind kind, String mask, int length, String termMask, String guide) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.kind = kind;
        this.mask = mask;
        this.length = length;
        this.termMask = termMask;
        this.guide = guide;
    }

    Spec() {
    }

    public String nodeId() { return nodeId; }
    public String nodeName() { return nodeName; }
    public String page() { return page; }
    public Kind kind() { return kind; }
    public String mask() { return mask; }
    public int length() { return length; }
    public String termMask() { return termMask; }
    public String mentCode() { return mentCode; }
    public String guide() { return guide; }

    /** 허용 키를 마스크 순서대로 중복 없이 반환한다. */
    public List<String> keys() {
        Set<String> seen = new LinkedHashSet<String>();
        for (int i = 0; i < mask.length(); i += Character.charCount(mask.codePointAt(i))) {
            seen.add(new String(Character.toChars(mask.codePointAt(i))));
        }
        return new ArrayList<String>(seen);
    }

    /** 호출 페이지와 자릿수로 입력 종류를 정한다. */
    static Kind kindOf(String page, int length) throws AppException {
        String p = page.toLowerCase(Locale.ROOT);
        if (p.equals(MENU_PAGE) && length == 1) {
            return Kind.MENU;
        }
        if (p.equals(MENU_PAGE)) {
            throw new AppException("menu page with length " + length);
        }
        if (p.startsWith("inputdtmf") && p.endsWith(".xml")) {
            return Kind.NUMBER;
        }
        throw new AppException("unsupported input page " + StringUtil.goQuote(page));
    }

    /** 마스크가 0-9, *, # 로만 구성되었는지 확인한다. */
    static boolean validMask(String mask) {
        return !mask.isEmpty() && mask.matches("[0-9*#]+");
    }
}
