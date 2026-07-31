from pathlib import Path

from docx import Document


ROOT = Path("docs/project")
CHECKS = {
    "01-EventFlow产品需求文档.docx": "7. 已确认的 MVP 规则",
    "02-EventFlow核心流程与状态机.docx": "6. 已冻结的流程边界",
    "04-EventFlow测试与验收方案.docx": "5. 已确认的压测与验收基线",
}


def main() -> int:
    valid = True
    for filename, expected_text in CHECKS.items():
        document = Document(ROOT / filename)
        content = "\n".join(paragraph.text for paragraph in document.paragraphs)
        matched = expected_text in content
        print(f"{filename}: {matched}")
        valid = valid and matched
    return 0 if valid else 1


if __name__ == "__main__":
    raise SystemExit(main())
