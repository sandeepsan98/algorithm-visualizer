package com.project.codeflowpro.servelts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@WebServlet("/execute")
public class ExecuteServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteServlet.class);
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_INPUT = "64, 34, 25, 12, 22, 11, 90"; // 7 numbers
    private static final int TIMEOUT_SECONDS = 15;
    private static final int BASE_LOG_INTERVAL = 10;
    private static final int MAX_INPUT_SIZE = 50;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");

        StringBuilder jsonBuffer = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }

        JsonObject json;
        try {
            json = GSON.fromJson(jsonBuffer.toString(), JsonObject.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid JSON format\"}");
            LOGGER.error("Invalid JSON received: {}", e.getMessage());
            return;
        }

        String code = json.get("code").getAsString();
        String language = json.get("language").getAsString();

        String inputPattern = language.equals("Java") ? "\\{%input%\\}" : "\\[%input%\\]";
        String input = code.contains(inputPattern) ? code.split(inputPattern)[1].split(language.equals("Java") ? "\\}" : "\\]")[0].trim() : "";
        boolean isNumeric = false;
        String formattedInput;
        int inputSize;

        if (input.isEmpty()) {
            formattedInput = DEFAULT_INPUT;
            isNumeric = true;
            inputSize = 7;
        } else {
            String[] parts = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (parts.length == 0) {
                formattedInput = DEFAULT_INPUT;
                isNumeric = true;
                inputSize = 7;
            } else {
                // Validate 1 to 50 elements
                if (parts.length < 1 || parts.length > MAX_INPUT_SIZE) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Input must be between 1 and 50 elements\"}");
                    LOGGER.error("Invalid input: must be between 1 and 50 elements, got {}", parts.length);
                    return;
                }
                // Check if all are numbers (for range validation) or strings
                isNumeric = Arrays.stream(parts).allMatch(s -> s.matches("\\d+"));
                if (isNumeric) {
                    int[] numbers = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
                    if (Arrays.stream(numbers).anyMatch(n -> n < 1 || n > 100)) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("{\"error\": \"Numeric inputs must be between 1 and 100\"}");
                        LOGGER.error("Invalid input: numeric values out of range 1-100");
                        return;
                    }
                }
                // Format input based on type
                formattedInput = language.equals("Java")
                        ? (isNumeric ? String.join(", ", parts) : "\"" + String.join("\", \"", parts) + "\"")
                        : (isNumeric ? String.join(",", parts) : "'" + String.join("','", parts) + "'");
                inputSize = parts.length;
            }
        }
        code = code.replaceFirst(inputPattern + ".*?(?=\\}|\\])", language.equals("Java") ? "{" + formattedInput + "}" : "[" + formattedInput + "]");

        // Dynamic log interval based on input size
        int logInterval = Math.max(BASE_LOG_INTERVAL, inputSize / 5);
        LOGGER.info("Input size: {}, Log interval: {}", inputSize, logInterval);

        String output = "";
        JsonArray steps = new JsonArray();
        String[] algorithmTypeHolder = {"Unknown"};
        long executionTimeMs = 0;

        try {
            long startTime = System.nanoTime();
            switch (language) {
                case "JavaScript":
                    String resultJS = executeJavaScript(code, req);
                    output = parseOutputAndSteps(resultJS, steps, algorithmTypeHolder, code, false);
                    break;
                case "Java":
                    String resultJava = executeJava(code);
                    output = parseOutputAndSteps(resultJava, steps, algorithmTypeHolder, code, true);
                    inferAlgorithmType(code, algorithmTypeHolder);
                    break;
                default:
                    throw new IllegalStateException("Unsupported language: " + language);
            }
            executionTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        } catch (Exception e) {
            LOGGER.error("Execution error for {}: {}", language, e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(String.format("{\"error\": \"Execution failed: %s\"}", e.getMessage()));
            return;
        }

        resp.getWriter().write(String.format(
                "{\"output\": \"%s\", \"steps\": %s, \"algorithm\": \"%s\", \"executionTime\": %d, \"isNumeric\": %b, \"status\": \"success\"}",
                output.replace("\"", "\\\""), GSON.toJson(steps), algorithmTypeHolder[0], executionTimeMs, isNumeric
        ));
    }

    private String executeJavaScript(String code, HttpServletRequest req) throws IOException, InterruptedException {
        String scriptPath = req.getServletContext().getRealPath("/js/execute.js");
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            LOGGER.error("execute.js not found at: {}", scriptPath);
            return "Error: execute.js not found at " + scriptPath;
        }
        ProcessBuilder pb = new ProcessBuilder("node", scriptPath, code);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        if (!p.waitFor(20, TimeUnit.SECONDS)) { // Increased timeout
            p.destroy();
            LOGGER.warn("JavaScript execution timed out");
            return "Execution timeout exceeded";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    private String executeJava(String code) throws IOException, InterruptedException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File tempFile = new File(tempDir, "Temp.java");
        if (tempFile.exists()) tempFile.delete();

        String[] lines = code.split("\n");
        StringBuilder instrumentedCode = new StringBuilder();
        int braceCount = 0;
        boolean inMethod = false;
        boolean initialStateLogged = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("{")) braceCount += line.chars().filter(ch -> ch == '{').count();
            if (line.contains("}")) braceCount -= line.chars().filter(ch -> ch == '}').count();

            if (line.startsWith("public class Temp")) {
                instrumentedCode.append(lines[i]).append("\n");
            } else if (line.startsWith("public static") || line.startsWith("static")) {
                inMethod = true;
                instrumentedCode.append(lines[i]).append("\n");
            } else if (inMethod && braceCount > 0 && !line.isEmpty() && !line.equals("}") && !line.startsWith("System.out.println(\"LINE:")) {
                if (!initialStateLogged && line.contains("arr = {")) {
                    instrumentedCode.append("    ").append(line).append("\n");
                    instrumentedCode.append("    System.out.println(\"LINE:").append(i + 1).append("\");\n");
                    instrumentedCode.append("    logState(arr, " + (i + 1) + ");\n");
                    initialStateLogged = true;
                } else if (!line.contains("logState")) {
                    instrumentedCode.append("    System.out.println(\"LINE:").append(i + 1).append("\");\n");
                    // Log state on array modifications
                    if (line.matches(".*arr\\s*\\[.*\\]\\s*=.*;")) {
                        instrumentedCode.append("    ").append(line).append("\n");
                        instrumentedCode.append("    logState(arr, " + (i + 1) + ");\n");
                    } else {
                        instrumentedCode.append("    ").append(line).append("\n");
                    }
                } else {
                    instrumentedCode.append("    ").append(line.replace("logState(arr)", "logState(arr, " + (i + 1) + ")")).append("\n");
                }
            } else {
                instrumentedCode.append(lines[i]).append("\n");
            }

            if (inMethod && braceCount == 0 && line.equals("}")) {
                inMethod = false;
            }
        }

        String logStateMethods = """
            public static void logState(Object[] arr, int line) {
                System.out.println("STATE:" + java.util.Arrays.toString(arr) + ":LINE:" + line);
            }
            public static void logState(int[] arr, int line) {
                System.out.println("STATE:" + java.util.Arrays.toString(arr) + ":LINE:" + line);
            }
        """;
        int lastBraceIndex = instrumentedCode.lastIndexOf("}");
        if (lastBraceIndex != -1) {
            instrumentedCode.insert(lastBraceIndex, logStateMethods);
        } else {
            instrumentedCode.append(logStateMethods).append("}\n");
        }

        Files.writeString(tempFile.toPath(), instrumentedCode.toString());

        ProcessBuilder pb = new ProcessBuilder("javac", tempFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process compileProcess = pb.start();
        compileProcess.waitFor(20, TimeUnit.SECONDS); // Increased timeout
        String compileOutput = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        if (compileProcess.exitValue() != 0) {
            LOGGER.error("Java compilation error: {}", compileOutput);
            tempFile.delete();
            return "Compilation error: " + compileOutput;
        }

        pb = new ProcessBuilder("java", "-cp", tempDir.getAbsolutePath(), "Temp");
        pb.redirectErrorStream(true);
        Process runProcess = pb.start();
        if (!runProcess.waitFor(20, TimeUnit.SECONDS)) { // Increased timeout
            runProcess.destroy();
            tempFile.delete();
            return "Execution timeout exceeded";
        }
        String output = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))
                .lines().collect(Collectors.joining("\n"));
        tempFile.delete();
        LOGGER.info("Java execution output: {}", output);
        return output;
    }

    private String parseOutputAndSteps(String result, JsonArray steps, String[] algorithmTypeHolder, String code, boolean isJava) {
        String[] lines = result.split("\n");
        String finalOutput = "";

        for (String line : lines) {
            if (line.startsWith("LINE:")) {
                int lineNumber = Integer.parseInt(line.substring(5));
                JsonObject step = new JsonObject();
                step.addProperty("line", lineNumber);
                steps.add(step);
            } else if (line.startsWith("STATE:")) {
                String[] parts = line.split(":LINE:");
                String stateStr = parts[0].substring(6);
                int lineNumber = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                try {
                    JsonObject step = new JsonObject();
                    step.add("array", GSON.fromJson(stateStr, JsonArray.class));
                    step.addProperty("line", lineNumber);
                    steps.add(step);
                } catch (Exception e) {
                    LOGGER.error("Failed to parse state: {}", stateStr, e);
                }
            } else if (line.startsWith("ALGORITHM_TYPE:")) {
                algorithmTypeHolder[0] = line.substring(15);
            } else if (!line.trim().isEmpty() && !line.startsWith("DEBUG:") && !line.startsWith("Error:")) {
                finalOutput = line;
            }
        }
        return finalOutput.isEmpty() ? "No output" : finalOutput;
    }

    private void inferAlgorithmType(String code, String[] algorithmTypeHolder) {
        if (code.contains("partition") && code.contains("quickSort")) {
            algorithmTypeHolder[0] = "Quick Sort";
        } else if (code.contains("while") && code.contains("key") && !code.contains("partition")) {
            algorithmTypeHolder[0] = "Insertion Sort";
        } else if (code.contains("merge") && !code.contains("partition") && !code.contains("heapify")) {
            algorithmTypeHolder[0] = "Merge Sort";
        } else if (code.contains("heapify") && !code.contains("partition")) {
            algorithmTypeHolder[0] = "Heap Sort";
        } else if (code.contains("length - i - 1")) {
            algorithmTypeHolder[0] = "Bubble Sort";
        } else if (code.contains("minIdx")) {
            algorithmTypeHolder[0] = "Selection Sort";
        }
    }
}