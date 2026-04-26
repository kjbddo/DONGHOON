#!/usr/bin/env python3
"""
DB의 문제 텍스트에서 literal `\n`, `\t`, `\r` 두 글자를 실제 제어문자로 정규화한다.

KaTeX 명령(`\max`, `\le` ...)을 보존하기 위해 `$...$` 와 `$$...$$` 영역은 건드리지 않는다.

대상:
- problems.description / input_description / output_description (text)
- problems.constraints / examples (jsonb, 문자열 노드 전체 재귀 변환)
- test_cases.input / expected_output (text)

idempotent. 변경된 행만 UPDATE.

사용 (호스트에서, docker postgres 컨테이너 사용):

  sudo python3 infra/scripts/normalize_problem_text.py

신규 응답은 ai-server (`chains/problem_gen_chain.py::_normalize_payload`) 와
backend (`com.algoforge.backend.ai.util.AiTextNormalizer`) 가 막아주므로 1회성으로 충분하다.
"""

import json
import re
import subprocess
import sys
import textwrap
from typing import Any

CONTAINER = "algoforge-postgres"

PG_RAW = ["docker", "exec", "-i", CONTAINER,
          "psql", "-U", "algoforge", "-d", "algoforge",
          "-X", "-v", "ON_ERROR_STOP=1"]

MATH_SPLIT = re.compile(r"(\$\$[\s\S]*?\$\$|\$[^\n$]+\$)")


def _replace_literal_escapes(s: str) -> str:
    if "\\" not in s:
        return s
    return (
        s.replace("\\r\\n", "\n")
         .replace("\\n", "\n")
         .replace("\\r", "\n")
         .replace("\\t", "\t")
    )


def normalize_text(value: str) -> str:
    if not isinstance(value, str):
        return value
    parts = MATH_SPLIT.split(value)
    for i in range(0, len(parts), 2):
        parts[i] = _replace_literal_escapes(parts[i])
    return "".join(parts)


def normalize_payload(node: Any) -> Any:
    if isinstance(node, str):
        return normalize_text(node)
    if isinstance(node, list):
        return [normalize_payload(x) for x in node]
    if isinstance(node, dict):
        return {k: normalize_payload(v) for k, v in node.items()}
    return node


def run_psql_json_one_row(query: str) -> Any:
    p = subprocess.run([*PG_RAW, "-At", "-c", query],
                       capture_output=True, text=True, check=True)
    out = p.stdout.strip()
    if not out:
        return None
    return json.loads(out)


def run_psql_apply(sql: str) -> None:
    p = subprocess.run(PG_RAW, input=sql, capture_output=True, text=True)
    if p.returncode != 0:
        sys.stderr.write(p.stderr)
        raise SystemExit(p.returncode)
    if p.stdout.strip():
        print(p.stdout.strip())


def fetch_problems():
    return run_psql_json_one_row(textwrap.dedent("""
        SELECT COALESCE(jsonb_agg(t ORDER BY id), '[]'::jsonb)::text
          FROM (
            SELECT id, description, input_description, output_description,
                   constraints, examples
              FROM problems
          ) t;
    """))


def fetch_test_cases():
    return run_psql_json_one_row(textwrap.dedent("""
        SELECT COALESCE(jsonb_agg(t ORDER BY id), '[]'::jsonb)::text
          FROM (
            SELECT id, input, expected_output
              FROM test_cases
          ) t;
    """))


def quote_lit(s: str) -> str:
    """PostgreSQL 표준 문자열 리터럴 escape (E'...')."""
    return ("E'" + s.replace("\\", "\\\\").replace("'", "''")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "'")


def quote_jsonb(node: Any) -> str:
    return quote_lit(json.dumps(node, ensure_ascii=False)) + "::jsonb"


def main():
    problems = fetch_problems() or []
    test_cases = fetch_test_cases() or []

    updates = []
    changed_rows = 0

    for row in problems:
        original = {
            "description": row.get("description") or "",
            "input_description": row.get("input_description") or "",
            "output_description": row.get("output_description") or "",
            "constraints": row.get("constraints") or [],
            "examples": row.get("examples") or [],
        }
        fixed = {
            "description": normalize_text(original["description"]),
            "input_description": normalize_text(original["input_description"]),
            "output_description": normalize_text(original["output_description"]),
            "constraints": normalize_payload(original["constraints"]),
            "examples": normalize_payload(original["examples"]),
        }
        if fixed != original:
            changed_rows += 1
            sets = []
            for col, val in fixed.items():
                if val == original[col]:
                    continue
                if col in ("constraints", "examples"):
                    sets.append(f"{col} = {quote_jsonb(val)}")
                else:
                    sets.append(f"{col} = {quote_lit(val)}")
            sets.append("updated_at = NOW()")
            updates.append(f"UPDATE problems SET {', '.join(sets)} WHERE id = {row['id']};")

    tc_changed = 0
    for tc in test_cases:
        original = {
            "input": tc.get("input") or "",
            "expected_output": tc.get("expected_output") or "",
        }
        fixed = {
            "input": normalize_text(original["input"]),
            "expected_output": normalize_text(original["expected_output"]),
        }
        if fixed != original:
            tc_changed += 1
            sets = []
            for col, val in fixed.items():
                if val != original[col]:
                    sets.append(f"{col} = {quote_lit(val)}")
            updates.append(f"UPDATE test_cases SET {', '.join(sets)} WHERE id = {tc['id']};")

    print(f"problems rows changed: {changed_rows} / {len(problems)}")
    print(f"test_cases rows changed: {tc_changed} / {len(test_cases)}")

    if not updates:
        print("Nothing to update.")
        return

    sql = "BEGIN;\n" + "\n".join(updates) + "\nCOMMIT;\n"
    run_psql_apply(sql)
    print("Done.")


if __name__ == "__main__":
    main()
