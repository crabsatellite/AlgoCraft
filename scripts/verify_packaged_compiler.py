"""Exercise the release JAR's isolated compiler without a development ECJ classpath."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess, tempfile, zipfile

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--jar', type=Path, required=True)
p.add_argument('--java', type=Path, required=True)
p.add_argument('--gson', type=Path, required=True)
p.add_argument('--output', type=Path, required=True)
a = p.parse_args()
NOWIN = 0x08000000 if os.name == 'nt' else 0
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()

with zipfile.ZipFile(a.jar) as jar:
    assert jar.testzip() is None
    assert 'assets/algocraft/compiler/ecj.jar' in jar.namelist()
    assert not any(n.startswith('META-INF/jarjar/') and 'ecj' in n for n in jar.namelist())
    if 'META-INF/jarjar/metadata.json' in jar.namelist():
        metadata = json.loads(jar.read('META-INF/jarjar/metadata.json'))
        assert not any(e.get('identifier', {}).get('artifact') == 'ecj' for e in metadata['jars'])

HELPER = r'''
import com.crabmods.algocraft.logic.CodeExecutor;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
public class PackagedCompilerProbe {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler");
            throw new AssertionError("ECJ unexpectedly exists on the parent classpath");
        } catch (ClassNotFoundException expected) { }
        System.setProperty("algocraft.codeExecutor.forceEcjCompiler", "true");
        System.setProperty("algocraft.codeExecutorTimeoutMs", "8000");
        JsonArray fixtures = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
        int total = 0;
        for (JsonElement fixture : fixtures) {
            JsonObject problem = fixture.getAsJsonObject();
            String id = problem.get("id").getAsString();
            String code = problem.getAsJsonArray("solutions").get(0).getAsJsonObject().get("code").getAsString();
            List<CodeExecutor.TestCase> tests = new ArrayList<>();
            for (JsonElement element : problem.getAsJsonArray("tests")) {
                JsonObject test = element.getAsJsonObject();
                tests.add(new CodeExecutor.TestCase(test.get("input").getAsString(), test.get("output").getAsString(), id));
            }
            List<CodeExecutor.TestResult> results = CodeExecutor.executeBatch(code, tests);
            if (results.size() != tests.size()) throw new AssertionError("Incomplete results for " + id);
            for (CodeExecutor.TestResult result : results) {
                if (!result.passed) throw new AssertionError(id + ": " + result.message);
            }
            total += results.size();
        }
        System.out.println("PACKAGED_COMPILER_PASSED " + total);
    }
}
'''
ids = ['1', '7', '73', '95', '496', '500']
fixtures = [json.loads((ROOT / f'question_bank/official/p{i}.json').read_text(encoding='utf8')) for i in ids]
with tempfile.TemporaryDirectory(prefix='algocraft-packaged-compiler-') as temporary:
    work = Path(temporary)
    source = work / 'PackagedCompilerProbe.java'
    source.write_text(HELPER, encoding='utf8')
    (work / 'fixtures.json').write_text(json.dumps(fixtures), encoding='utf8')
    cp = os.pathsep.join([str(a.jar.resolve()), str(a.gson.resolve())])
    compiled = subprocess.run([str(a.java.with_name('javac.exe' if os.name == 'nt' else 'javac')),
                               '-cp', cp, '-d', str(work), str(source)],
                              capture_output=True, text=True, creationflags=NOWIN, timeout=60)
    assert compiled.returncode == 0, compiled.stderr
    process = subprocess.run([str(a.java), '-Xmx512m', '-XX:ActiveProcessorCount=2',
                              '-Djava.io.tmpdir='+str(work), '-cp', os.pathsep.join([str(work), cp]),
                              'PackagedCompilerProbe', str(work / 'fixtures.json')],
                             capture_output=True, text=True, creationflags=NOWIN, timeout=240)
    assert process.returncode == 0, process.stdout + process.stderr
    assert 'PACKAGED_COMPILER_PASSED' in process.stdout, process.stdout
    count = int(process.stdout.strip().split()[-1])

receipt = {'passed': True, 'jarSha256': sha(a.jar), 'javaExecutable': str(a.java),
           'problems': ids, 'testCases': count, 'ecjAbsentFromParentClasspath': True,
           'compilerLoadedFromBundledResourceByWorker': True, 'headless': True}
a.output.parent.mkdir(parents=True, exist_ok=True)
a.output.write_text(json.dumps(receipt, indent=2), encoding='utf8')
print(json.dumps(receipt, indent=2))
