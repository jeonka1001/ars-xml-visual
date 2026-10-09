package com.wavve.arsxml2wv.wv;

/**
 * WV 화면코드. 운영 XML 의 기존 WV 노드에서 추정했다.
 * labels.properties 의 &lt;Id&gt;.screen 으로 임의 코드를 지정할 수 있어 enum 대신 문자열 상수로 둔다.
 */
public final class ScreenCode {
    private ScreenCode() {
    }

    /** 공통 종목코드 검색 */
    public static final String STOCK_SEARCH = "SHKC10";
    /** 숫자 입력 */
    public static final String NUMBER_INPUT = "SHKC21";
    /** 확인·선택 메뉴 (거래 처리 전) */
    public static final String CONFIRM = "SHKD12";
    /** 처리 완료 메뉴 (거래 처리 후) */
    public static final String COMPLETE = "SHKE00";
}
