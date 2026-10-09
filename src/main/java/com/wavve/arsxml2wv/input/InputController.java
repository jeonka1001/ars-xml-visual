package com.wavve.arsxml2wv.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.Tsv;
import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.diagram.Node;

/** 입력 노드 수집·저장 진입점. 해석은 InputService 에 위임한다. */
public final class InputController {
    private InputController() {
    }

    /** 다이어그램의 입력 노드를 모두 해석한다. 해석하지 못한 노드는 warnings 에 남긴다. */
    public static List<Spec> collect(Diagram d, List<String> warnings) {
        List<Spec> specs = new ArrayList<Spec>();
        for (Node n : d.nodes()) {
            if (!InputService.isInputNode(n)) {
                continue;
            }
            try {
                specs.add(InputService.fromNode(n));
            } catch (AppException e) {
                warnings.add("node " + n.id() + ": skipped: " + e.getMessage());
            }
        }
        return specs;
    }

    /** outDir 에 inputs.tsv 를 저장한다. */
    public static void exportSpecs(List<Spec> specs, String outDir) throws IOException {
        Tsv.save(outDir, "inputs.tsv", InputService.specsTsv(specs));
    }
}
