package com.wavve.arsxml2wv.diagram;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.Tsv;

/** 파일 입출력 진입점. 해석은 DiagramService 에 위임한다. */
public final class DiagramController {
    private DiagramController() {
    }

    /** XML 파일을 열어 DiagramService.parse 에 위임한다. */
    public static Diagram load(String path) throws AppException {
        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            try {
                return DiagramService.parse(in);
            } catch (AppException e) {
                throw new AppException(path + ": " + e.getMessage(), e);
            }
        } catch (NoSuchFileException e) {
            throw new AppException("open " + path + ": file not found", e);
        } catch (IOException e) {
            throw new AppException("open " + path + ": " + e.getMessage(), e);
        }
    }

    /** outDir 에 nodes.tsv, links.tsv 를 저장한다. */
    public static void exportInventory(Diagram d, String outDir) throws IOException {
        Tsv.save(outDir, "nodes.tsv", DiagramService.nodesTsv(d));
        Tsv.save(outDir, "links.tsv", DiagramService.linksTsv(d));
    }
}
