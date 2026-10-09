package com.wavve.arsxml2wv.common;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class StringUtilTest {
    @Test
    public void trimSpaceMatchesGo() {
        assertEquals("가 나", StringUtil.trimSpace(" \t 가 나\r\n　"));
        assertEquals("\u001Fa", StringUtil.trimSpace("\u001Fa "));
        assertEquals("", StringUtil.trimSpace(" \n "));
    }

    @Test
    public void fieldsMatchesGo() {
        assertEquals(Arrays.asList("종목코드", "입력"), StringUtil.fields(" 종목코드\r\n입력 "));
        assertEquals("초기 메뉴 이동", StringUtil.oneLine("초기\n메뉴  이동"));
    }

    @Test
    public void tsvCleansCells() {
        Tsv t = new Tsv("a", "b");
        t.row("x\ty", "1\r\n2");
        assertEquals("a\tb\nx y\t1 2\n", t.toString());
    }
}
