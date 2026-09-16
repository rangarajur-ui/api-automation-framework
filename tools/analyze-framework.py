#!/usr/bin/env python3
"""
Static analyzer for this Java TestNG / Rest Assured / Selenium framework.

Produces explorer/model.json from source. Relationships are marked HIGH when
both ends exist in this repo, MEDIUM when a call target is resolved by simple
name, LOW when only a string/pattern was seen.

Does not print or store secret values.
"""

from __future__ import annotations

import json
import re
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC_ROOTS = [ROOT / "src" / "main" / "java", ROOT / "src" / "test" / "java"]
OUT = ROOT / "explorer" / "model.json"

SENSITIVE_KEY = re.compile(
    r"(token|password|secret|card|cvv|credential|api[_-]?key|access[_-]?key)", re.I
)
MOBILE_KEY = re.compile(r"(mobile|phone|msisdn)", re.I)
STORY_SKIP = re.compile(
    r"^(config\.|api\.ApiCall#|api\.ApiRequestSpec#|tests\.report\.|"
    r"utils\.TokenMasker#|tests\.support\.CheckoutStats#|pojo\.)"
)

CLASS_RE = re.compile(
    r"(?P<ann>(?:\s*@\w+(?:\([^;]*?\))?\s*)*)"
    r"(?P<vis>public|protected|private)?\s*"
    r"(?P<mod>(?:static|final|abstract)\s+)*"
    r"(?P<kind>class|interface|enum)\s+(?P<name>\w+)"
    r"(?:\s+extends\s+(?P<ext>[\w.]+))?"
    r"(?:\s+implements\s+(?P<impl>[\w.,\s]+))?",
    re.M,
)
METHOD_RE = re.compile(
    r"(?P<ann>(?:^[ \t]*@[\w.]+(?:\([^;]*?\))?[ \t]*\n)+)?"
    r"^[ \t]*(?P<vis>public|protected|private)\s+"
    r"(?P<mod>(?:static|final|synchronized)\s+)*"
    r"(?P<ret>[\w.<>,\[\] ?]+)\s+(?P<name>\w+)\s*\((?P<params>[^)]*)\)",
    re.M,
)
IMPORT_RE = re.compile(r"^import\s+(static\s+)?([\w.]+);", re.M)
PACKAGE_RE = re.compile(r"^package\s+([\w.]+);", re.M)
CALL_RE = re.compile(r"(?<![\w.])([A-Z]\w+)\.(\w+)\s*\(")
NEW_CALL_RE = re.compile(r"new\s+([A-Z]\w+)\s*\([^;]*?\)\s*\.\s*(\w+)\s*\(")
THIS_CALL_RE = re.compile(r"(?<![\w.])(\w+)\s*\(")
ASSIGN_NEW_RE = re.compile(r"(\w+)\s*=\s*new\s+([A-Z]\w+)\s*\(")
INST_CALL_RE = re.compile(r"(?<![\w.])([a-z]\w*)\.(\w+)\s*\(")
JAVA_WORDS = {
    "if", "for", "while", "switch", "catch", "return", "new", "assert",
    "super", "this", "else", "try", "throw", "synchronized", "case",
}
HTTP_RE = re.compile(r"\.(post|get|put|patch|delete)\(\s*\"(/[^\"]+)\"", re.I)
HTTP_CONCAT_RE = re.compile(
    r"\.(post|get|put|patch|delete)\(\s*\"(/[^\"]+)\"\s*\+", re.I
)
BY_RE = re.compile(r"By\.(id|cssSelector|xpath|name|className)\(\s*\"([^\"]+)\"")
GROUP_RE = re.compile(r"groups\s*=\s*\{([^}]+)\}")
DP_RE = re.compile(r"dataProvider\s*=\s*\"([^\"]+)\"")
DP_NAME_RE = re.compile(r"@DataProvider(?:\s*\(\s*name\s*=\s*\"([^\"]+)\")?")
TESTNG_NAME_RE = re.compile(r"name\s*=\s*\"([^\"]+)\"")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//.*?$", "", text, flags=re.M)


def split_params(raw: str) -> list[dict]:
    if not raw.strip():
        return []
    parts = []
    depth = 0
    current = []
    for ch in raw:
        if ch in "<(":
            depth += 1
        elif ch in ">)":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    if "".join(current).strip():
        parts.append("".join(current).strip())
    out = []
    for part in parts:
        bits = part.split()
        if not bits:
            continue
        out.append({"type": " ".join(bits[:-1]) or bits[0], "name": bits[-1]})
    return out


def annotations_block(block: str) -> list[dict]:
    found = []
    for m in re.finditer(r"@(\w+)(?:\((.*?)\))?", block or "", re.S):
        name = m.group(1)
        args = (m.group(2) or "").strip()
        found.append({"name": name, "args": args[:400]})
    return found


def groups_from(args: str) -> list[str]:
    m = GROUP_RE.search(args or "")
    if not m:
        return []
    return [g.strip().strip("\"'") for g in m.group(1).split(",") if g.strip()]


def mask_value(key: str, value: str) -> tuple[str, bool]:
    raw = value.strip()
    if MOBILE_KEY.search(key):
        digits = re.sub(r"\D", "", raw)
        if len(digits) >= 4:
            return f"****{digits[-4:]}", True
        return "********", True
    if SENSITIVE_KEY.search(key):
        return "********", True
    return raw, False


def read_properties(path: Path) -> list[dict]:
    rows = []
    if not path.exists():
        return rows
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        shown, masked = mask_value(key, value)
        rows.append({"key": key, "value": shown, "masked": masked})
    return rows


def parse_testng_xml(path: Path) -> dict:
    data = {"file": str(path.relative_to(ROOT)), "name": path.stem, "listeners": [], "classes": [], "groups": []}
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        return data
    root = tree.getroot()
    data["name"] = root.attrib.get("name", path.stem)
    for listener in root.findall(".//listener"):
        cls = listener.attrib.get("class-name")
        if cls:
            data["listeners"].append(cls)
    for include in root.findall(".//include"):
        name = include.attrib.get("name")
        if name:
            data["groups"].append(name)
    for cls in root.findall(".//class"):
        name = cls.attrib.get("name")
        if name:
            data["classes"].append(name)
    return data


def parse_java(path: Path) -> dict | None:
    raw = path.read_text(encoding="utf-8", errors="replace")
    text = strip_comments(raw)
    pkg_m = PACKAGE_RE.search(text)
    package = pkg_m.group(1) if pkg_m else ""
    imports = [m.group(2) for m in IMPORT_RE.finditer(text)]
    class_m = CLASS_RE.search(text)
    if not class_m:
        return None
    kind = class_m.group("kind")
    name = class_m.group("name")
    extends = class_m.group("ext")
    impl = class_m.group("impl")
    implements = [p.strip() for p in impl.split(",")] if impl else []
    fqcn = f"{package}.{name}" if package else name
    rel = str(path.relative_to(ROOT))
    lines = raw.splitlines()
    class_ann = annotations_block(class_m.group("ann") or "")

    methods = []
    for m in METHOD_RE.finditer(text):
        if m.group("name") == name:
            continue
        start = text[: m.start()].count("\n") + 1
        anns = annotations_block(m.group("ann") or "")
        args = " ".join(a["args"] for a in anns)
        methods.append(
            {
                "id": f"{fqcn}#{m.group('name')}",
                "name": m.group("name"),
                "visibility": m.group("vis"),
                "static": "static" in (m.group("mod") or ""),
                "returnType": m.group("ret").strip(),
                "params": split_params(m.group("params")),
                "annotations": anns,
                "groups": groups_from(args),
                "dataProvider": (DP_RE.search(args) or [None, None])[1],
                "line": start,
                "isTest": any(a["name"] == "Test" for a in anns),
                "isDataProvider": any(a["name"] == "DataProvider" for a in anns),
                "isListenerHook": any(
                    a["name"] in {"Override"} and m.group("name").startswith(("on", "before", "after", "generate"))
                    for a in anns
                ),
                "excerpt": "\n".join(lines[max(0, start - 1) : start + 14]),
            }
        )

    http = []
    for m in HTTP_CONCAT_RE.finditer(text):
        http.append({"method": m.group(1).upper(), "path": m.group(2) + "{…}", "confidence": "HIGH"})
    for m in HTTP_RE.finditer(text):
        path_s = m.group(2)
        if not any(h["path"].startswith(path_s) for h in http):
            http.append({"method": m.group(1).upper(), "path": path_s, "confidence": "HIGH"})

    locators = [
        {"strategy": m.group(1), "value": m.group(2), "confidence": "HIGH"}
        for m in BY_RE.finditer(text)
    ]

    calls = []
    seen_calls = set()
    for rx in (CALL_RE, NEW_CALL_RE):
        for m in rx.finditer(text):
            target_cls, target_m = m.group(1), m.group(2)
            if target_m in {"equals", "toString", "hashCode", "valueOf", "parseInt", "parseDouble"}:
                continue
            key = (target_cls, target_m)
            if key in seen_calls:
                continue
            seen_calls.add(key)
            calls.append({"class": target_cls, "method": target_m})

    selenium = any(
        x in imports or x in text
        for x in ("org.openqa.selenium", "ChromeDriver", "WebDriverWait", "By.")
    )
    rest = "io.restassured" in " ".join(imports) or ".given(" in text
    testng = "org.testng" in " ".join(imports) or any(a["name"] == "Test" for meth in methods for a in meth["annotations"])

    return {
        "id": fqcn,
        "name": name,
        "package": package,
        "kind": kind,
        "file": rel,
        "lineCount": len(lines),
        "extends": extends,
        "implements": implements,
        "imports": imports,
        "annotations": class_ann,
        "methods": methods,
        "http": http,
        "locators": locators,
        "calls": calls,
        "sourceExcerpt": "\n".join(lines[:60]),
        "source": raw,
        "isTestClass": any(meth["isTest"] for meth in methods),
        "isListener": any("Listener" in i for i in implements) or "ITestListener" in implements or "ISuiteListener" in implements,
        "isApiClient": rest and not any(meth["isTest"] for meth in methods),
        "isPageObject": selenium,
        "isPojo": kind == "class" and package.startswith("pojo"),
        "usesRestAssured": rest,
        "usesSelenium": selenium,
        "usesTestNg": testng,
        "layer": classify_layer(package, name, rest, selenium, methods),
    }


def classify_layer(package: str, name: str, rest: bool, selenium: bool, methods: list) -> str:
    if any(m["isTest"] for m in methods):
        return "test"
    if "Listener" in name or "Reporter" in name or "Catalog" in name:
        return "reporting"
    if selenium:
        return "ui"
    if package.startswith("api") or name.endswith("Api"):
        return "api"
    if package.startswith("config"):
        return "config"
    if package.startswith("pojo"):
        return "model"
    if package.startswith("utils"):
        return "util"
    if package.startswith("tests.support"):
        return "support"
    return "other"


def main() -> None:
    classes = []
    for root in SRC_ROOTS:
        for path in sorted(root.rglob("*.java")):
            parsed = parse_java(path)
            if parsed:
                classes.append(parsed)

    by_name = {c["name"]: c for c in classes}
    by_fqcn = {c["id"]: c for c in classes}
    method_ids = {m["id"]: m for c in classes for m in c["methods"]}

    relationships = []

    seen_rel = set()

    def rel(src, tgt, typ, confidence="HIGH", note=None):
        key = (src, tgt, typ)
        if key in seen_rel:
            return
        seen_rel.add(key)
        relationships.append(
            {"source": src, "target": tgt, "type": typ, "confidence": confidence, "note": note}
        )

    folders = {}
    for c in classes:
        folder = str(Path(c["file"]).parent)
        folders.setdefault(folder, []).append(c["id"])
        rel(folder, c["file"], "FOLDER_CONTAINS")
        rel(c["file"], c["id"], "FILE_CONTAINS")
        rel(c["id"], c["file"], "DEFINED_IN")
        for meth in c["methods"]:
            rel(c["id"], meth["id"], "CONTAINS_METHOD")
        if c["extends"] and c["extends"] in by_name:
            rel(c["id"], by_name[c["extends"]]["id"], "EXTENDS")
        for iface in c["implements"]:
            if iface in by_name:
                rel(c["id"], by_name[iface]["id"], "IMPLEMENTS")
            else:
                rel(c["id"], iface, "IMPLEMENTS", "MEDIUM", "External or unresolved type")
        for imp in c["imports"]:
            simple = imp.split(".")[-1]
            if simple in by_name:
                rel(c["id"], by_name[simple]["id"], "IMPORTS")

    for c in classes:
        for call in c["calls"]:
            target = by_name.get(call["class"])
            if not target:
                continue
            mid = f"{target['id']}#{call['method']}"
            if mid in method_ids:
                # attach to nearest test/method later; class-level CALLS
                rel(c["id"], mid, "CALLS", "HIGH")
            else:
                rel(c["id"], target["id"], "USES", "MEDIUM", f"Call {call['class']}.{call['method']}()")

    # Method-body call resolution: map test methods to called repo methods
    for c in classes:
        lines = c["source"].splitlines()
        method_starts = sorted(m["line"] for m in c["methods"])
        for meth in c["methods"]:
            start = meth["line"] - 1
            nxt = next((s - 1 for s in method_starts if s > meth["line"]), len(lines))
            body = "\n".join(lines[start:nxt])
            def link_call(tcls: str, tmeth: str, confidence="HIGH"):
                target = by_name.get(tcls)
                if not target:
                    return
                mid = f"{target['id']}#{tmeth}"
                if mid in method_ids:
                    rel(meth["id"], mid, "CALLS", confidence)
                else:
                    rel(meth["id"], target["id"], "USES", "MEDIUM")

            hits = []
            for rx in (CALL_RE, NEW_CALL_RE):
                for cm in rx.finditer(body):
                    hits.append((cm.start(), cm.group(1), cm.group(2), "type"))
            created = {am.group(1): am.group(2) for am in ASSIGN_NEW_RE.finditer(body)}
            for cm in INST_CALL_RE.finditer(body):
                tcls = created.get(cm.group(1))
                if tcls:
                    hits.append((cm.start(), tcls, cm.group(2), "type"))
            own = {m["name"] for m in c["methods"]}
            for cm in THIS_CALL_RE.finditer(body):
                name = cm.group(1)
                if name in JAVA_WORDS or name == meth["name"] or name not in own:
                    continue
                hits.append((cm.start(), c["name"], name, "own"))
            hits.sort(key=lambda h: h[0])
            for _pos, tcls, tmeth, kind in hits:
                if kind == "own":
                    rel(meth["id"], f"{c['id']}#{tmeth}", "CALLS", "HIGH")
                else:
                    link_call(tcls, tmeth)
            seen_http = set()
            for http in HTTP_CONCAT_RE.finditer(body):
                node = f"HTTP {http.group(1).upper()} {http.group(2)}{{…}}"
                seen_http.add((http.group(1).upper(), http.group(2)))
                rel(meth["id"], node, "CONNECTS_TO", "HIGH")
            for http in HTTP_RE.finditer(body):
                key = (http.group(1).upper(), http.group(2))
                if key in seen_http:
                    continue
                rel(meth["id"], f"HTTP {http.group(1).upper()} {http.group(2)}", "CONNECTS_TO", "HIGH")

    suites = []
    for xml in sorted((ROOT / "src" / "test" / "resources").glob("testng*.xml")):
        suite = parse_testng_xml(xml)
        suites.append(suite)
        for listener in suite["listeners"]:
            if listener in by_fqcn:
                rel(suite["file"], listener, "USES", "HIGH")
            else:
                rel(suite["file"], listener, "USES", "MEDIUM")
        for cls in suite["classes"]:
            if cls in by_fqcn:
                rel(suite["file"], cls, "CONTAINS", "HIGH")

    groups = defaultdict(list)
    for c in classes:
        for meth in c["methods"]:
            for g in meth["groups"]:
                groups[g].append(meth["id"])

    data_providers = []
    for c in classes:
        for meth in c["methods"]:
            if meth["isDataProvider"]:
                dp_name = meth["name"]
                for a in meth["annotations"]:
                    nm = TESTNG_NAME_RE.search(a.get("args") or "")
                    if nm:
                        dp_name = nm.group(1)
                consumers = [
                    m["id"]
                    for oc in classes
                    for m in oc["methods"]
                    if m.get("dataProvider") == dp_name
                ]
                data_providers.append(
                    {"id": meth["id"], "name": dp_name, "file": c["file"], "consumers": consumers}
                )
                for consumer in consumers:
                    rel(consumer, meth["id"], "USES", "HIGH", "TestNG DataProvider")

    # Config keys (masked)
    configs = {
        "config.properties": read_properties(ROOT / "src/test/resources/config.properties"),
        "config-staging.properties": read_properties(ROOT / "src/test/resources/config-staging.properties"),
        "testdata.properties": read_properties(ROOT / "src/test/resources/testdata.properties"),
        "testdata-staging.properties": read_properties(ROOT / "src/test/resources/testdata-staging.properties"),
    }

    # Maven profiles
    pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    profiles = re.findall(r"<id>([^<]+)</id>\s*<properties>\s*<suite.file>([^<]+)</suite.file>", pom)

    tests = []
    for c in classes:
        for meth in c["methods"]:
            if not meth["isTest"]:
                continue
            callees = [r["target"] for r in relationships if r["source"] == meth["id"] and r["type"] in {"CALLS", "USES", "CONNECTS_TO"}]
            tests.append(
                {
                    "id": meth["id"],
                    "name": meth["name"],
                    "class": c["id"],
                    "file": c["file"],
                    "groups": meth["groups"],
                    "dataProvider": meth["dataProvider"],
                    "http": [r["target"] for r in relationships if r["source"] == meth["id"] and r["type"] == "CONNECTS_TO"],
                    "calls": callees,
                    "line": meth["line"],
                }
            )

    def outgoing(start: str):
        return [
            r
            for r in relationships
            if r["source"] == start
            and r["type"] in {"CALLS", "CONNECTS_TO"}
            and r["confidence"] == "HIGH"
        ]

    def walk(start: str, depth=0, seen=None):
        seen = seen if seen is not None else set()
        if start in seen or depth > 6:
            return []
        seen.add(start)
        steps = [start]
        for r in outgoing(start):
            steps.extend(walk(r["target"], depth + 1, seen))
        return steps

    def is_story_node(nid: str, depth: int) -> bool:
        if depth == 0 or nid.startswith("HTTP "):
            return True
        return STORY_SKIP.search(nid) is None

    def story_walk(start: str):
        steps = []
        seen = set()

        def visit(nid: str, depth: int = 0):
            if nid in seen or depth > 8:
                return
            seen.add(nid)
            if is_story_node(nid, depth):
                steps.append(nid)
            for r in outgoing(nid):
                visit(r["target"], depth + 1)

        visit(start)
        return steps

    traces = {}
    story_traces = {}
    for t in tests:
        traces[t["id"]] = walk(t["id"])
        story = story_walk(t["id"])
        story_traces[t["id"]] = story
        joined = " ".join(story)
        if "TelrHostedPage" in joined or "CheckoutFlow" in joined:
            t["domain"] = "checkout"
        elif any(x.startswith("api.") or x.startswith("HTTP ") for x in story):
            t["domain"] = "api"
        else:
            t["domain"] = "shared"

    endpoints = []
    seen_ep = set()
    for c in classes:
        for h in c["http"]:
            key = (h["method"], h["path"])
            if key in seen_ep:
                continue
            seen_ep.add(key)
            endpoints.append({**h, "class": c["id"], "file": c["file"]})

    def http_for(method: str, prefix: str) -> str:
        for e in endpoints:
            if e["method"] == method and e["path"].startswith(prefix):
                return f"HTTP {e['method']} {e['path']}"
        return f"HTTP {method} {prefix}"

    checkout_path = [
        "tests.QrOrderFlowTest#shouldCreateAcceptedOrderAfterTelrPayment",
        "api.QrMenuApi#getQrMenuDetails",
        http_for("POST", "/qr_online_order/"),
        "api.CartApi#viewCart",
        http_for("POST", "/qr_view_cart/"),
        "tests.support.CheckoutFlow#payAndConfirm",
        "api.PaymentApi#initiatePayment",
        http_for("POST", "/initiate_payment/"),
        "ui.TelrHostedPage#completePaymentAndReturnPaytmUrl",
        "api.PaymentApi#fetchPaymentStatus",
        http_for("GET", "/fetch_payment_status_by_session_id/"),
        "api.CartApi#viewCustomerCart",
        "api.OmsApi#getOrderDetails",
        http_for("GET", "/m/oms/orders/"),
        "tests.report.ExtentReportListener",
    ]
    checkout_path = [p for p in checkout_path if p in method_ids or p in by_fqcn or p.startswith("HTTP ")]

    locators = []
    for c in classes:
        for loc in c["locators"]:
            locators.append({**loc, "class": c["id"], "file": c["file"]})

    insights = []
    # large classes
    for c in classes:
        if c["lineCount"] > 250:
            insights.append(
                {
                    "severity": "info",
                    "title": f"{c['name']} is {c['lineCount']} lines",
                    "detail": "Potential large class. Detected from file length, not a confirmed defect.",
                    "node": c["id"],
                }
            )
        incoming = sum(1 for r in relationships if r["target"] == c["id"])
        if incoming >= 8:
            insights.append(
                {
                    "severity": "info",
                    "title": f"{c['name']} has {incoming} incoming relationships",
                    "detail": "Highly connected in this repo. Detected from import/call graph.",
                    "node": c["id"],
                }
            )
    sleep_files = [c["file"] for c in classes if "Thread.sleep" in c["source"]]
    if sleep_files:
        insights.append(
            {
                "severity": "info",
                "title": "Fixed delay detected",
                "detail": (
                    "Thread.sleep appears in "
                    + ", ".join(sleep_files)
                    + ". Potential synchronization improvement, not a confirmed defect."
                ),
            }
        )

    # circular at class IMPORTS level
    import_graph = defaultdict(set)
    for r in relationships:
        if r["type"] == "IMPORTS" and r["source"] in by_fqcn and r["target"] in by_fqcn:
            import_graph[r["source"]].add(r["target"])
    cycles = []
    for a, outs in import_graph.items():
        for b in outs:
            if a in import_graph.get(b, set()) and a < b:
                cycles.append([a, b, a])
    if cycles:
        insights.append(
            {
                "severity": "warn",
                "title": f"{len(cycles)} mutual import pair(s)",
                "detail": "Possible circular dependency at import level. Confirm before treating as a defect.",
                "cycles": cycles,
            }
        )

    test_methods = [m for c in classes for m in c["methods"] if m["isTest"]]
    api_tests = [t for t in tests if any("HTTP" in x for x in t["http"]) or t["class"] in {
        "tests.SmokeTests", "tests.SanityTests", "tests.NegativeTests", "tests.RegressionTests",
        "tests.CustomizationItemTests", "tests.CartVariantTests", "tests.CustomerDetailsTests",
        "tests.InstructionTests", "tests.QrOrderFlowTest",
    }]
    ui_classes = [c for c in classes if c["usesSelenium"]]

    # Strip full source from payload except excerpt + selected windows to keep JSON smaller
    slim_classes = []
    for c in classes:
        slim = dict(c)
        slim.pop("source", None)
        slim.pop("calls", None)
        slim_classes.append(slim)

    model = {
        "project": {
            "name": "api-automation-framework",
            "build": "Maven",
            "java": "21",
            "testFramework": "TestNG 7.12.0",
            "api": "Rest Assured 5.5.6",
            "ui": "Selenium 4.35.0 (Telr hosted page only)",
            "reporting": "ExtentReports 5.1.2",
            "purpose": "Prove a dine-in QR guest can pay on Telr and the same order appears in OMS.",
            "defaultCommand": "mvn test",
            "defaultEnv": "test",
        },
        "stats": {
            "javaFiles": len(classes),
            "classes": len([c for c in classes if c["kind"] == "class"]),
            "interfaces": len([c for c in classes if c["kind"] == "interface"]),
            "methods": len(method_ids),
            "testClasses": len([c for c in classes if c["isTestClass"]]),
            "testMethods": len(test_methods),
            "apiClients": len([c for c in classes if c["isApiClient"]]),
            "pageObjects": len(ui_classes),
            "listeners": len([c for c in classes if c["isListener"]]),
            "dataProviders": len(data_providers),
            "endpoints": len(endpoints),
            "suites": len(suites),
            "groups": len(groups),
            "relationships": len(relationships),
            "packages": len({c["package"] for c in classes}),
        },
        "packages": sorted({c["package"] for c in classes}),
        "folders": {k: v for k, v in folders.items()},
        "classes": slim_classes,
        "tests": tests,
        "suites": suites,
        "groups": {k: v for k, v in groups.items()},
        "dataProviders": data_providers,
        "endpoints": endpoints,
        "locators": locators,
        "relationships": relationships,
        "traces": traces,
        "storyTraces": story_traces,
        "checkoutFlow": checkout_path,
        "configs": configs,
        "profiles": [{"id": i, "suite": s} for i, s in profiles],
        "insights": insights,
        "external": [
            {"id": "Paytm QR API", "kind": "external-api", "note": "biz.test / biz.staging hosts from config.properties"},
            {"id": "Telr Hosted Page", "kind": "external-ui", "note": "Card page opened by Selenium"},
            {"id": "OMS Back Office", "kind": "external-api", "note": "portal.test or dashboard.staging from config"},
        ],
        "entryPoints": [
            {"id": "cli-smoke", "label": "mvn test", "kind": "CLI", "target": "src/test/resources/testng-smoke.xml"},
            {"id": "cli-regression", "label": "mvn test -Pregression", "kind": "CLI", "target": "src/test/resources/testng-regression.xml"},
            {"id": "cli-e2e", "label": "mvn test -Pe2e", "kind": "CLI", "target": "src/test/resources/testng-e2e.xml"},
            {"id": "jenkins", "label": "Jenkinsfile", "kind": "CI", "target": "Jenkinsfile"},
            {"id": "gha-api", "label": "GitHub Actions API (smoke + negative)", "kind": "CI", "target": ".github/workflows/smoke.yml"},
            {"id": "gha-suite", "label": "GitHub Actions test suite", "kind": "CI", "target": ".github/workflows/suite.yml"},
            {"id": "e2e-test", "label": "QrOrderFlowTest", "kind": "TEST", "target": "tests.QrOrderFlowTest#shouldCreateAcceptedOrderAfterTelrPayment"},
        ],
        "lifecycleFound": sorted(
            {
                a["name"]
                for c in classes
                for m in c["methods"]
                for a in m["annotations"]
                if a["name"]
                in {
                    "BeforeSuite",
                    "AfterSuite",
                    "BeforeTest",
                    "AfterTest",
                    "BeforeClass",
                    "AfterClass",
                    "BeforeMethod",
                    "AfterMethod",
                    "Test",
                    "DataProvider",
                    "Listeners",
                }
            }
        ),
        "coverageNote": "Coverage data not available. Showing static test relationships instead.",
        "analysis": {
            "method": "Custom Java parser (declarations, imports, annotations, invocations, HTTP strings, Selenium By).",
            "notFullCompiler": True,
            "knownLimits": [
                "Method bodies are windowed (~220 lines) so very long methods may miss later calls.",
                "Overloaded methods collapse to one id (class#name).",
                "External library types are not fully resolved.",
            ],
        },
    }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(model, indent=2), encoding="utf-8")
    print(f"Wrote {OUT} classes={len(classes)} tests={len(tests)} rels={len(relationships)}")


if __name__ == "__main__":
    main()
