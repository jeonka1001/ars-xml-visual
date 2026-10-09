package com.wavve.arsxml2wv.wv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.Tsv;
import com.wavve.arsxml2wv.diagram.Node;
import com.wavve.arsxml2wv.input.Kind;
import com.wavve.arsxml2wv.input.Spec;
import com.wavve.arsxml2wv.label.Button;
import com.wavve.arsxml2wv.label.LabelResult;

/** 화면 생성·저장 진입점. 로직은 WvService 에 위임한다. */
public final class WvController {
    private WvController() {
    }

    /** 다이어그램 노드 순서대로 화면을 만든다. 만들지 못한 노드는 notes 에 남긴다. */
    public static List<Screen> buildAll(WvInput in, List<String> notes) {
        Map<String, Spec> specs = new HashMap<String, Spec>();
        for (Spec s : in.specs) {
            specs.put(s.nodeId(), s);
        }
        Map<String, List<Button>> buttons = new HashMap<String, List<Button>>();
        for (LabelResult r : in.buttons) {
            buttons.put(r.nodeId(), r.buttons());
        }
        Set<String> reach = WvService.reachableAfterUpdate(in.diagram);
        List<Screen> screens = new ArrayList<Screen>();
        for (Node n : in.diagram.nodes()) {
            try {
                Screen sc = buildOne(in, n, specs.get(n.id()), buttons.get(n.id()), reach, notes);
                if (sc != null) {
                    screens.add(sc);
                }
            } catch (AppException e) {
                notes.add("node " + n.id() + ": screen skipped: " + e.getMessage());
            }
        }
        return screens;
    }

    private static Screen buildOne(WvInput in, Node n, Spec s, List<Button> buttons, Set<String> reach, List<String> notes)
            throws AppException {
        if (WvService.isStockSearch(n)) {
            return WvService.stockSearchScreen(in, n, notes);
        }
        if (s == null) {
            return null;
        }
        if (s.kind() == Kind.MENU) {
            List<Button> b = buttons == null ? new ArrayList<Button>() : buttons;
            return WvService.menuScreen(in, s, b, reach, notes);
        }
        return WvService.numberScreen(in, s, notes);
    }

    /** 화면마다 JS 파일을 outDir 에 저장한다. */
    public static void export(List<Screen> screens, String outDir) throws IOException {
        for (int i = 0; i < screens.size(); i++) {
            Screen s = screens.get(i);
            Tsv.save(outDir, s.fileName(i + 1), WvService.render(s));
        }
    }
}
