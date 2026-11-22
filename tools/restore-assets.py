"""Restore original frontend media from the companion ZIP using Python 3.11+."""
import argparse
from hashlib import file_digest
import json
from pathlib import Path, PurePosixPath
import shutil
from tempfile import TemporaryDirectory
from zipfile import BadZipFile, ZipFile


def matches(path, entry):
    if not path.is_file() or path.stat().st_size != entry["bytes"]:
        return False
    with path.open("rb") as stream:
        return file_digest(stream, "sha256").hexdigest() == entry["sha256"]


def restore(archive_path):
    root = Path(__file__).resolve().parents[1]
    public = root / "frontend" / "public"
    index = json.loads((root / "docs" / "模型与视频索引.json").read_text(encoding="utf-8"))
    entries = {item["path"]: item for item in index["media"] if item["storage"] == "asset-archive"}

    with ZipFile(archive_path) as archive, TemporaryDirectory(dir=public) as directory:
        names = archive.namelist()
        if len(names) != len(entries) or set(names) != set(entries):
            raise ValueError("The ZIP does not match this checkout's media index.")

        pending = []
        for name, entry in entries.items():
            path = PurePosixPath(name)
            if path.parts[:2] != ("frontend", "public") or len(path.parts) != 3 or path.name == "..":
                raise ValueError(f"Unexpected media path: {name}")

            destination = public / path.name
            if destination.exists() or destination.is_symlink():
                if not destination.is_symlink() and matches(destination, entry):
                    continue
                raise FileExistsError(f"Existing file differs: {destination}")

            if archive.getinfo(name).file_size != entry["bytes"]:
                raise ValueError(f"Size mismatch: {name}")
            staged = Path(directory) / path.name
            with archive.open(name) as source, staged.open("xb") as target:
                shutil.copyfileobj(source, target)
            if not matches(staged, entry):
                raise ValueError(f"SHA-256 mismatch: {name}")
            pending.append((staged, destination))

        for staged, destination in pending:
            destination.hardlink_to(staged)

    print(f"Restored {len(pending)} files. {len(entries) - len(pending)} already matched.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    args = parser.parse_args()
    try:
        restore(args.archive)
    except (BadZipFile, OSError, ValueError, RuntimeError) as error:
        parser.exit(1, f"{error}\n")
