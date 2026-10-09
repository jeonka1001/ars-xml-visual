package com.wavve.arsxml2wv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 합성 시나리오 End-to-End 회귀 테스트. 기대 결과(golden)는 Go 버전이 만든 파일이며 바이트 단위로 비교한다.
 * 의도한 변경이면 결과 검토 후 갱신: mvn test -Dupdate=true
 */
public class MainTest {
    private static final File E2E = new File("src/test/resources/e2e");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void runMatchesGolden() throws Exception {
        File out = tmp.newFolder("out");
        Main.run(new File(E2E, "scenario.xml").getPath(), out.getPath(), new File(E2E, "labels.properties").getPath());
        File golden = new File(E2E, "golden");
        if (Boolean.getBoolean("update")) {
            writeGolden(golden, out);
        }
        String[] want = golden.list();
        String[] got = out.list();
        Arrays.sort(want);
        Arrays.sort(got);
        assertArrayEquals(want, got);
        for (String name : want) {
            assertEquals(name, read(new File(golden, name)), read(new File(out, name)));
        }
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    private static void writeGolden(File golden, File out) throws Exception {
        for (File f : golden.listFiles()) {
            Files.delete(f.toPath());
        }
        for (File f : out.listFiles()) {
            Files.copy(f.toPath(), new File(golden, f.getName()).toPath());
        }
    }
}
