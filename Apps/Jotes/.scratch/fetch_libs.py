"""Resolve e baixa JARs do Maven Central para lib/<categoria>/."""
import json, os, sys, urllib.request

# (group, artifact, categoria em lib/)
LIBS = [
    ("org.pushingpixels", "trident", "anim"),
    ("com.miglayout", "miglayout-core", "ui"),
    ("com.miglayout", "miglayout-swing", "ui"),
    ("com.github.weisj", "jsvg", "ui"),
    ("com.fifesoft", "autocomplete", "textarea"),
    ("org.jsoup", "jsoup", "extract"),
    ("io.github.java-diff-utils", "java-diff-utils", "text"),
    ("org.apache.commons", "commons-text", "text"),
    ("org.apache.commons", "commons-lang3", "text"),
    ("org.slf4j", "slf4j-api", "log"),
    ("ch.qos.logback", "logback-core", "log"),
    ("ch.qos.logback", "logback-classic", "log"),
    ("org.junit.platform", "junit-platform-console-standalone", "test"),
    ("org.apache.pdfbox", "pdfbox", "pdf"),
    ("org.apache.pdfbox", "fontbox", "pdf"),
    ("org.apache.tika", "tika-core", "extract"),
    ("org.quartz-scheduler", "quartz", "services"),
]

def latest(g, a):
    for attempt in (0, 1):
        try:
            url = (f"https://search.maven.org/solrsearch/select?q=g:%22{g}%22+AND+a:%22{a}%22"
                   f"&rows=20&core=gav&wt=json")
            with urllib.request.urlopen(url, timeout=60) as r:
                data = json.load(r)
            break
        except Exception:
            if attempt: raise
    docs = data["response"]["docs"]
    vers = [d["v"] for d in docs if "v" in d]
    # prefere a versão estável mais alta sem sufixos alfa/beta/rc
    def key(v):
        import re
        parts = re.split(r"[.\-]", v)
        return [int(p) if p.isdigit() else -1 for p in parts]
    stable = [v for v in vers if all(x not in v.lower() for x in ("alpha", "beta", "rc", "m", "snapshot"))]
    pick = stable or vers
    return sorted(pick, key=key)[-1] if pick else None

ok, fail = [], []
for g, a, cat in LIBS:
    try:
        v = latest(g, a)
        if not v:
            fail.append((g, a, "not found")); continue
        path = g.replace(".", "/")
        url = f"https://repo1.maven.org/maven2/{path}/{a}/{v}/{a}-{v}.jar"
        os.makedirs(f"lib/{cat}", exist_ok=True)
        dest = f"lib/{cat}/{a}-{v}.jar"
        if os.path.exists(dest):
            ok.append(dest); continue
        urllib.request.urlretrieve(url, dest)
        ok.append(dest)
        print(f"OK  {dest} ({os.path.getsize(dest)//1024} KB)")
    except Exception as e:
        fail.append((g, a, str(e)))

for g, a, why in fail:
    print(f"FAIL {g}:{a} -> {why}")
sys.exit(1 if fail else 0)
