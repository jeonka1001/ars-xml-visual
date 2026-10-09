package com.wavve.arsxml2wv.label;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.Tsv;
import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.input.Kind;
import com.wavve.arsxml2wv.input.Spec;

/** 버튼 문구 결정·저장 진입점. 로직은 LabelService 에 위임한다. */
public final class LabelController {
    private LabelController() {
    }

    /** labels.properties 를 읽는다. path 가 비어 있으면 빈 설정이다. */
    public static Map<String, String> loadOverrides(String path) throws AppException {
        if (path == null || path.isEmpty()) {
            return new LinkedHashMap<String, String>();
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            return LabelService.parseProperties(new String(bytes, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            throw new AppException("open " + path + ": file not found", e);
        } catch (IOException e) {
            throw new AppException("open " + path + ": " + e.getMessage(), e);
        }
    }

    /** 메뉴 노드의 버튼 문구를 정하고, 사람이 확인할 항목을 notes 에 남긴다. */
    public static List<LabelResult> resolveAll(Diagram d, List<Spec> specs, Map<String, String> overrides, List<String> notes) {
        List<LabelResult> results = new ArrayList<LabelResult>();
        for (Spec s : specs) {
            if (s.kind() != Kind.MENU) {
                continue;
            }
            LabelResult r = LabelService.resolve(d, s, overrides);
            results.add(r);
            notes.addAll(notesOf(r));
        }
        return results;
    }

    private static List<String> notesOf(LabelResult r) {
        List<String> notes = new ArrayList<String>();
        for (Button b : r.buttons()) {
            String head = "node " + r.nodeId() + " key " + b.key() + ": ";
            if (!b.resolved()) {
                notes.add(head + "no label; button omitted (add " + r.nodeId() + ".button." + b.key() + ")");
            } else if (b.inferred()) {
                notes.add(head + "label from " + b.source().label() + ", review wording");
            }
        }
        return notes;
    }

    /** outDir 에 buttons.tsv 를 저장한다. */
    public static void exportButtons(List<LabelResult> results, String outDir) throws IOException {
        Tsv.save(outDir, "buttons.tsv", LabelService.buttonsTsv(results));
    }
}
