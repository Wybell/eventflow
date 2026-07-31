from pathlib import Path
import sys

from docx import Document


def main() -> int:
    root = Path(sys.argv[1])
    files = sorted(root.rglob("*.docx"))
    errors: list[str] = []
    for path in files:
        try:
            document = Document(path)
            text = "\n".join(paragraph.text for paragraph in document.paragraphs).strip()
            if not text:
                errors.append(f"empty text: {path}")
            if not path.read_bytes().startswith(b"PK"):
                errors.append(f"not a DOCX zip: {path}")
        except Exception as exc:
            errors.append(f"{path}: {exc}")
    print(f"Validated DOCX files: {len(files)}")
    if errors:
        print("\n".join(errors))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
