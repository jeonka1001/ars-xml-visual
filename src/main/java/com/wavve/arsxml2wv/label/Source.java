package com.wavve.arsxml2wv.label;

/** 버튼 문구를 어디서 가져왔는지. */
public enum Source {
    /** labels.properties */
    OVERRIDE("override"),
    /** PreScript 안내 주석 */
    COMMENT("comment"),
    /** 입력체크 Switch 분기의 도착 노드 이름 */
    BRANCH("branch"),
    /** 찾지 못함 */
    NONE("none");

    private final String label;

    Source(String label) {
        this.label = label;
    }

    /** 검토 파일에 쓰는 이름. */
    public String label() {
        return label;
    }
}
