package com.wavve.arsxml2wv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wavve.arsxml2wv.common.AppException;
import com.wavve.arsxml2wv.common.StringUtil;
import com.wavve.arsxml2wv.common.Tsv;
import com.wavve.arsxml2wv.diagram.Diagram;
import com.wavve.arsxml2wv.diagram.DiagramController;
import com.wavve.arsxml2wv.input.InputController;
import com.wavve.arsxml2wv.input.Spec;
import com.wavve.arsxml2wv.label.LabelController;
import com.wavve.arsxml2wv.label.LabelResult;
import com.wavve.arsxml2wv.wv.Screen;
import com.wavve.arsxml2wv.wv.WvController;
import com.wavve.arsxml2wv.wv.WvInput;

/**
 * Hansol 시나리오 XML 을 읽어 보이는 ARS(WV) 스크립트 초안을 만든다.
 * 사용법: arsxml2wv &lt;input.xml&gt; &lt;output-dir&gt; [labels.properties]
 * 콘솔 메시지는 영문으로 둔다. 한글 Windows 콘솔(CP949)에서 UTF-8 한글이 깨지기 때문이다.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.print("usage: arsxml2wv <input.xml> <output-dir> [labels.properties]\n");
            System.exit(2);
        }
        try {
            run(args[0], args[1], args.length == 3 ? args[2] : "");
        } catch (AppException | IOException e) {
            System.err.print("error: " + e.getMessage() + "\n");
            System.exit(1);
        }
    }

    static void run(String xmlPath, String outDir, String labelsPath) throws AppException, IOException {
        Map<String, String> overrides = LabelController.loadOverrides(labelsPath);
        Diagram d = DiagramController.load(xmlPath);
        List<String> review = new ArrayList<String>(d.warnings());
        List<Spec> specs = InputController.collect(d, review);
        List<LabelResult> buttons = LabelController.resolveAll(d, specs, overrides, review);
        List<Screen> screens = WvController.buildAll(new WvInput(d, specs, buttons, overrides), review);
        DiagramController.exportInventory(d, outDir);
        InputController.exportSpecs(specs, outDir);
        LabelController.exportButtons(buttons, outDir);
        Tsv.save(outDir, "review.txt", StringUtil.join(review, "\n") + "\n");
        WvController.export(screens, outDir);
        System.out.print(String.format("nodes: %d, links: %d, input nodes: %d, screens: %d, review items: %d\n",
                d.nodes().size(), d.links().size(), specs.size(), screens.size(), review.size()));
        System.out.print("see review.txt for items to check\n");
    }
}
