/*
 * Project 3 Functional Tester (strict ++)
 *
 * How to run:
 *   javac -encoding UTF-8 EmotionAnalyzer.java EmotionAnalyzerTester.java
 *   java EmotionAnalyzerTester
 *
 * What’s new vs. your original:
 * - HINTS toggle that prints targeted guidance when tests fail.
 * - Clearer diffs for formatting (shows invisible chars).
 * - Extra coverage:
 *     • classify boundaries: -1, 0, +1
 *     • normalize edge cases (curly apostrophe ’, digits, empty, hyphens)
 *     • mixed lines (pos+neg), repetition counts
 *     • summary tie logic + +0 sign formatting
 * - Reflection checks: POS/PW/NEG/NW are public static final arrays.
 */

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class EmotionAnalyzerTester {

    // ===== Instructor tuning =====
    private static final boolean HINTS = true;   // set false to suppress hints
    private static final boolean SHOW_STACK = false;

    // Canonical spec from the handout (STRICT)
    private static final String[] SPEC_POS = {"happy","wonderful","great","excited","love"};
    private static final int[]    SPEC_PW  = {4,2,2,3,3};
    private static final String[] SPEC_NEG = {"terrible","awful","sad","hate","angry"};
    private static final int[]    SPEC_NW  = {3,4,2,3,3};

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== EmotionAnalyzer Tester (strict++) ===");

        try {
            reflectionGuards();
        } catch (Throwable t) {
            fail("Reflection guards", "Reflection checks crashed: " + t.getMessage());
            maybePrintStack(t);
        }

        // 0) Enforce canonical arrays/weights before any scoring tests
        if (!checkCanonicalWeights()) {
            System.out.println();
            System.out.println("RESULT: " + passed + " passed, " + (++failed) + " failed");
            System.out.println("================================");
            return;
        }

        // 1) Functional tests
        testNormalizeTokenTable();
        testNormalizeEdgeCases();
        testIndexOf();
        testScoreLineBasics();
        testScoreLineMixed();
        testClassifyBoundaries();
        testWriteLineResult();
        testWriteSummary();
        testSummaryTieLogic();
        testEndToEndSample();

        System.out.println();
        System.out.printf("RESULT: %d passed, %d failed%n", passed, failed);
        System.out.println("================================");
    }

    // ---------- Reflection guards (lightweight) ----------
    private static void reflectionGuards() {
        System.out.println("\n-- reflectionGuards --");
        checkPSFArray("POS", String[].class);
        checkPSFArray("NEG", String[].class);
        checkPSFArray("PW",  int[].class);
        checkPSFArray("NW",  int[].class);
    }

    private static void checkPSFArray(String fieldName, Class<?> expectedType) {
        try {
            Field f = EmotionAnalyzer.class.getField(fieldName);
            int m = f.getModifiers();
            boolean ok = Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m)
                    && f.getType().equals(expectedType);
            if (ok) pass("Field '" + fieldName + "' is public static final " + expectedType.getSimpleName());
            else    fail("Field '" + fieldName + "'", "Must be public static final " + expectedType.getSimpleName());
        } catch (NoSuchFieldException e) {
            fail("Field '" + fieldName + "'", "Missing field");
        }
    }

    // ---------- Canonical check ----------
    private static boolean checkCanonicalWeights() {
        System.out.println("\n-- checkCanonicalWeights --");
        boolean ok = true;

        ok &= arraysEqual("POS keywords", SPEC_POS, EmotionAnalyzer.POS);
        ok &= intsEqual  ("POS weights",  SPEC_PW,  EmotionAnalyzer.PW);
        ok &= arraysEqual("NEG keywords", SPEC_NEG, EmotionAnalyzer.NEG);
        ok &= intsEqual  ("NEG weights",  SPEC_NW,  EmotionAnalyzer.NW);

        if (ok) pass("Canonical arrays and weights match the spec");
        else {
            fail("Canonical arrays/weights", "Your arrays or weights do not match the assignment spec");
            System.out.println();
            System.out.println("---- Expected vs. Found ----");
            printStringArrayDiff("POS", SPEC_POS, EmotionAnalyzer.POS);
            printIntArrayDiff   ("PW ", SPEC_PW , EmotionAnalyzer.PW );
            printStringArrayDiff("NEG", SPEC_NEG, EmotionAnalyzer.NEG);
            printIntArrayDiff   ("NW ", SPEC_NW , EmotionAnalyzer.NW );
            System.out.println("----------------------------");
            if (HINTS) {
                System.out.println("HINT: Do not reorder, rename, or change weights. The tester is strict by design.");
            }
        }
        return ok;
    }

    private static boolean arraysEqual(String name, String[] a, String[] b) {
        if (a == null || b == null || a.length != b.length) {
            fail(name, "Length mismatch or null");
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                fail(name, "Mismatch at index " + i + ": expected \"" + a[i] + "\" but got \"" + b[i] + "\"");
                return false;
            }
        }
        pass(name);
        return true;
    }

    private static boolean intsEqual(String name, int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            fail(name, "Length mismatch or null");
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail(name, "Mismatch at index " + i + ": expected " + a[i] + " but got " + b[i]);
                return false;
            }
        }
        pass(name);
        return true;
    }

    private static void printStringArrayDiff(String label, String[] exp, String[] got) {
        System.out.println(label + " expected: " + Arrays.toString(exp));
        System.out.println(label + " found   : " + Arrays.toString(got));
    }

    private static void printIntArrayDiff(String label, int[] exp, int[] got) {
        System.out.println(label + " expected: " + Arrays.toString(exp));
        System.out.println(label + " found   : " + Arrays.toString(got));
    }

    // ---------- Tiny assertion helpers ----------
    private static void assertEquals(String name, String expected, String actual) {
        if (Objects.equals(expected, actual)) pass(name);
        else {
            fail(name, "Expected: " + show(expected) + " but got: " + show(actual));
            if (HINTS && name.contains("writeLineResult")) {
                System.out.println("HINT: Use printf with: \"Line %d: %s (score %+d)%n\"");
                System.out.println("HINT: Ensure the '+' sign appears for positive and zero values.");
            }
        }
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected == actual) pass(name);
        else {
            fail(name, "Expected: " + expected + " but got: " + actual);
            if (HINTS && name.startsWith("scoreLine")) {
                System.out.println("HINT: Make sure you normalize each token, skip empty tokens,");
                System.out.println("      do a linear search in POS and NEG, then add/subtract weights.");
            }
        }
    }

    private static void assertTrue(String name, boolean cond, String msgIfFail) {
        if (cond) pass(name);
        else fail(name, msgIfFail);
    }

    private static void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name, String msg) {
        failed++;
        System.out.println("[FAIL] " + name + " -> " + msg);
    }

    private static String show(String s) {
        // Make invisible issues visible in diffs
        if (s == null) return "null";
        return "\"" + s
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }

    private static void maybePrintStack(Throwable t) {
        if (SHOW_STACK) t.printStackTrace(System.out);
    }

    // ---------- Tests ----------

    private static void testNormalizeTokenTable() {
        System.out.println("\n-- testNormalizeTokenTable --");
        String[][] cases = {
                {"Hello",       "hello"},
                {"DON'T",       "don't"},
                {"it's",        "it's"},
                {"'word'",      "word"},
                {"co-op",       "coop"},
                {"!!!Hi!!!",    "hi"},
                {"rock'n'roll", "rock'n'roll"}
        };
        for (int i = 0; i < cases.length; i++) {
            String in = cases[i][0];
            String expected = cases[i][1];
            String actual = EmotionAnalyzer.normalizeToken(in);
            assertEquals("normalizeToken["+i+"]: "+in, expected, actual);
        }
        if (HINTS) {
            System.out.println("HINT: Build a new StringBuilder; keep letters a..z and internal apostrophes; then trim edge apostrophes.");
        }
    }

    private static void testNormalizeEdgeCases() {
        System.out.println("\n-- testNormalizeEdgeCases --");
        // Curly apostrophe U+2019 should usually be removed, leaving letters only (students may keep it; spec says letters + apostrophe only)
        assertEquals("normalizeToken curly apostrophe",
                "im", EmotionAnalyzer.normalizeToken("I\u2019m")); // “I’m” -> “im” since ’ is not ASCII '
        assertEquals("normalizeToken digits",
                "lve", EmotionAnalyzer.normalizeToken("l0ve"));   // digits removed
        assertEquals("normalizeToken empty after strip",
                "", EmotionAnalyzer.normalizeToken("'''"));
        assertEquals("normalizeToken hyphen middle",
                "greatday", EmotionAnalyzer.normalizeToken("great-day"));
        assertEquals("normalizeToken apostrophe edges",
                "don't", EmotionAnalyzer.normalizeToken("'don't'").replace("’","")); // robust to either stripping behavior
    }

    private static void testIndexOf() {
        System.out.println("\n-- testIndexOf --");
        String[] arr = {"a","b","c"};
        assertEquals("indexOf: hit a", 0, EmotionAnalyzer.indexOf(arr, "a"));
        assertEquals("indexOf: hit c", 2, EmotionAnalyzer.indexOf(arr, "c"));
        assertEquals("indexOf: miss z", -1, EmotionAnalyzer.indexOf(arr, "z"));
    }

    private static void testScoreLineBasics() {
        System.out.println("\n-- testScoreLineBasics --");
        String[] pos = SPEC_POS;
        int[]    pw  = SPEC_PW;
        String[] neg = SPEC_NEG;
        int[]    nw  = SPEC_NW;

        assertEquals("scoreLine: 'happy' line",
                4, EmotionAnalyzer.scoreLine("I am so happy today!", pos, pw, neg, nw));
        assertEquals("scoreLine: 'terrible' line",
                -3, EmotionAnalyzer.scoreLine("This traffic is terrible.", pos, pw, neg, nw));
        assertEquals("scoreLine: 'wonderful' line",
                2, EmotionAnalyzer.scoreLine("What a wonderful morning.", pos, pw, neg, nw));
        assertEquals("scoreLine: 'awful' line",
                -4, EmotionAnalyzer.scoreLine("Ugh, everything feels awful.", pos, pw, neg, nw));
        assertEquals("scoreLine: 'excited' line",
                3, EmotionAnalyzer.scoreLine("I'm excited for the weekend!", pos, pw, neg, nw));
    }

    private static void testScoreLineMixed() {
        System.out.println("\n-- testScoreLineMixed --");
        String[] pos = SPEC_POS;
        int[]    pw  = SPEC_PW;
        String[] neg = SPEC_NEG;
        int[]    nw  = SPEC_NW;

        // Mixed: love(3) - sad(2) = +1
        assertEquals("scoreLine mixed love/sad",
                1, EmotionAnalyzer.scoreLine("I love this but feel sad too.", pos, pw, neg, nw));

        // Repeats: wonderful(2) + wonderful(2) - angry(3) = +1
        assertEquals("scoreLine repeats and neg",
                1, EmotionAnalyzer.scoreLine("Wonderful, simply wonderful... but also angry.", pos, pw, neg, nw));

        // Punctuation and spacing: great(2) + happy(4) - awful(4) = +2
        assertEquals("scoreLine punctuation spacing",
                2, EmotionAnalyzer.scoreLine("great!!!   happy... not awful.", pos, pw, neg, nw));
    }

    private static void testClassifyBoundaries() {
        System.out.println("\n-- testClassifyBoundaries --");
        assertEquals("classify -1", "Negative", EmotionAnalyzer.classify(-1));
        assertEquals("classify  0", "Neutral",  EmotionAnalyzer.classify(0));
        assertEquals("classify +1", "Positive", EmotionAnalyzer.classify(1));
    }

    private static void testWriteLineResult() {
        System.out.println("\n-- testWriteLineResult --");
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        EmotionAnalyzer.writeLineResult(out, 1, +4, "Positive");
        EmotionAnalyzer.writeLineResult(out, 2,  0, "Neutral");
        EmotionAnalyzer.writeLineResult(out, 3, -3, "Negative");
        out.flush();

        String ls = System.lineSeparator();
        String expected =
                "Line 1: Positive (score +4)" + ls +
                        "Line 2: Neutral (score +0)"  + ls +  // enforce +0 sign
                        "Line 3: Negative (score -3)" + ls;
        assertEquals("writeLineResult format", expected, sw.toString());
    }

    private static void testWriteSummary() {
        System.out.println("\n-- testWriteSummary --");
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        EmotionAnalyzer.writeSummary(out, 3, 2, 0);
        out.flush();

        String ls = System.lineSeparator();
        String expected =
                "----------------------------------" + ls +
                        "Positive: 3  Negative: 2  Neutral: 0" + ls +
                        "Overall sentiment: Positive :)" + ls;

        assertEquals("writeSummary block", expected, sw.toString());
    }

    private static void testSummaryTieLogic() {
        System.out.println("\n-- testSummaryTieLogic --");
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        // Tie: 2,2,1 -> Not a tie for top (Positive and Negative tied at 2, both > 1) => Neutral per spec
        EmotionAnalyzer.writeSummary(out, 2, 2, 1);
        out.flush();
        String ls = System.lineSeparator();
        String expected =
                "----------------------------------" + ls +
                        "Positive: 2  Negative: 2  Neutral: 1" + ls +
                        "Overall sentiment: Neutral :)" + ls;
        assertEquals("summary tie logic", expected, sw.toString());
    }

    private static void testEndToEndSample() {
        System.out.println("\n-- testEndToEndSample --");
        String ls = System.lineSeparator();
        String chat =
                "I am so happy today!" + ls +
                        "This traffic is terrible." + ls +
                        "What a wonderful morning." + ls +
                        "Ugh, everything feels awful." + ls +
                        "I’m excited for the weekend!" + ls;

        String expectedSummary =
                "Line 1: Positive (score +4)" + ls +
                        "Line 2: Negative (score -3)" + ls +
                        "Line 3: Positive (score +2)" + ls +
                        "Line 4: Negative (score -4)" + ls +
                        "Line 5: Positive (score +3)" + ls +
                        "----------------------------------" + ls +
                        "Positive: 3  Negative: 2  Neutral: 0" + ls +
                        "Overall sentiment: Positive :)" + ls;

        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        int positives = 0, negatives = 0, neutrals = 0;
        int lineNum = 0;

        Scanner file = new Scanner(chat);
        while (file.hasNextLine()) {
            String line = file.nextLine();
            lineNum++;
            int score = EmotionAnalyzer.scoreLine(line, SPEC_POS, SPEC_PW, SPEC_NEG, SPEC_NW);
            String label = EmotionAnalyzer.classify(score);

            if ("Positive".equals(label)) positives++;
            else if ("Negative".equals(label)) negatives++;
            else neutrals++;

            EmotionAnalyzer.writeLineResult(out, lineNum, score, label);
        }
        EmotionAnalyzer.writeSummary(out, positives, negatives, neutrals);
        out.flush();

        // Also write to a file so students see an artifact
        try (PrintWriter fileOut = new PrintWriter(new FileWriter("summary_test.txt"))) {
            fileOut.print(sw.toString());
        } catch (IOException e) {
            // not fatal
        }

        assertEquals("end-to-end summary match", expectedSummary, sw.toString());
        assertTrue("summary_test.txt created", new File("summary_test.txt").isFile(),
                "summary_test.txt not found after test");
    }
}
