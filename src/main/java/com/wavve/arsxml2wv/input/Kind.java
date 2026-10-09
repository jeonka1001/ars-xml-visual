package com.wavve.arsxml2wv.input;

/** 입력 노드의 종류. */
public enum Kind {
    /** 한 자리 메뉴 선택 (InputDTMF_Menu.xml) */
    MENU("menu"),
    /** 여러 자리 숫자 입력 (inputDTMF*.xml) */
    NUMBER("number");

    private final String label;

    Kind(String label) {
        this.label = label;
    }

    /** 검토 파일에 쓰는 이름. */
    public String label() {
        return label;
    }
}
