package com.wavve.arsxml2wv.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 검토용 TSV 문자열을 만들고 파일로 저장한다. */
public final class Tsv {
    private final StringBuilder b = new StringBuilder();

    /** 헤더 행이 들어간 표를 만든다. */
    public Tsv(String... header) {
        row(header);
    }

    /** 한 행을 추가한다. 셀 안의 탭·줄바꿈은 공백으로 바꾼다. */
    public void row(String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                b.append('\t');
            }
            b.append(clean(cells[i]));
        }
        b.append('\n');
    }

    private static String clean(String c) {
        return StringUtil.nz(c).replace("\t", " ").replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
    }

    @Override
    public String toString() {
        return b.toString();
    }

    /** dir/name 에 내용을 UTF-8 로 저장한다. dir 이 없으면 만든다. */
    public static void save(String dir, String name, String content) throws IOException {
        Path d = Paths.get(dir);
        Files.createDirectories(d);
        Files.write(d.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }
}
