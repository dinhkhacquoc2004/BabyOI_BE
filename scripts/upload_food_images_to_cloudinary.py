from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
import re
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_IMAGE_ROOT = Path(r"C:\Users\PC\Downloads\món ăn-20260601T081314Z-3-001")
DEFAULT_REPORT_PATH = ROOT / "build" / "food-cloudinary-upload-report.json"
DEFAULT_SQL_PATH = ROOT / "build" / "food-cloudinary-image-update.sql"
IMAGE_PATTERN = re.compile(r"^food_0*(\d+)\.(jpe?g|png|webp)$", re.IGNORECASE)
SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


@dataclass(frozen=True)
class CloudinaryConfig:
    cloud_name: str
    api_key: str
    api_secret: str
    folder: str


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    database: str
    username: str
    password: str


@dataclass(frozen=True)
class FoodImage:
    food_id: int
    path: Path
    public_id: str


def read_text_if_exists(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def read_yaml_value(text: str, key: str) -> str | None:
    # This is intentionally narrow: it reads the simple app config files in this repo
    # without adding a YAML dependency just for this maintenance script.
    match = re.search(rf"(?m)^\s*{re.escape(key)}:\s*[\"']?([^\"'\r\n#]+)", text)
    return match.group(1).strip() if match else None


def load_cloudinary_config(folder_override: str | None) -> CloudinaryConfig:
    app_yaml = read_text_if_exists(ROOT / "src" / "main" / "resources" / "application.yaml")
    cloud_name = os.getenv("CLOUDINARY_CLOUD_NAME") or read_yaml_value(app_yaml, "cloud-name")
    api_key = os.getenv("CLOUDINARY_API_KEY") or read_yaml_value(app_yaml, "api-key")
    api_secret = os.getenv("CLOUDINARY_API_SECRET") or read_yaml_value(app_yaml, "api-secret")
    folder = folder_override or os.getenv("CLOUDINARY_FOOD_FOLDER") or "food"

    missing = [
        name
        for name, value in {
            "CLOUDINARY_CLOUD_NAME": cloud_name,
            "CLOUDINARY_API_KEY": api_key,
            "CLOUDINARY_API_SECRET": api_secret,
        }.items()
        if not value
    ]
    if missing:
        raise SystemExit(f"Missing Cloudinary config: {', '.join(missing)}")

    return CloudinaryConfig(
        cloud_name=cloud_name,
        api_key=api_key,
        api_secret=api_secret,
        folder=folder.strip("/"),
    )


def load_db_config(profile: str | None) -> DbConfig:
    datasource_text = ""
    if profile:
        datasource_text = read_text_if_exists(ROOT / "src" / "main" / "resources" / f"application-{profile}.yaml")
    if not datasource_text:
        datasource_text = read_text_if_exists(ROOT / "src" / "main" / "resources" / "application-quoc.yaml")

    jdbc_url = os.getenv("SPRING_DATASOURCE_URL") or read_yaml_value(datasource_text, "url")
    username = os.getenv("SPRING_DATASOURCE_USERNAME") or read_yaml_value(datasource_text, "username")
    password = os.getenv("SPRING_DATASOURCE_PASSWORD") or read_yaml_value(datasource_text, "password")

    if not jdbc_url or not username or password is None:
        raise SystemExit(
            "Missing DB config. Set SPRING_DATASOURCE_URL, "
            "SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD."
        )

    match = re.match(r"jdbc:postgresql://([^:/]+):(\d+)/([^?]+)", jdbc_url)
    if not match:
        raise SystemExit(f"Unsupported SPRING_DATASOURCE_URL: {jdbc_url}")

    return DbConfig(
        host=match.group(1),
        port=int(match.group(2)),
        database=match.group(3),
        username=username,
        password=password,
    )


def discover_images(image_root: Path) -> list[FoodImage]:
    if not image_root.exists():
        raise SystemExit(f"Image folder does not exist: {image_root}")

    images: dict[int, FoodImage] = {}
    duplicates: dict[int, list[Path]] = {}

    for path in image_root.rglob("*"):
        if not path.is_file() or path.suffix.lower() not in SUPPORTED_EXTENSIONS:
            continue
        match = IMAGE_PATTERN.match(path.name)
        if not match:
            continue

        food_id = int(match.group(1))
        public_id = f"food_{food_id:03d}"
        item = FoodImage(food_id=food_id, path=path, public_id=public_id)

        if food_id in images:
            duplicates.setdefault(food_id, [images[food_id].path]).append(path)
            continue
        images[food_id] = item

    if duplicates:
        details = "; ".join(f"{food_id}: {paths}" for food_id, paths in sorted(duplicates.items()))
        raise SystemExit(f"Duplicate image IDs found: {details}")

    return [images[key] for key in sorted(images)]


def sign_cloudinary_params(params: dict[str, str], api_secret: str) -> str:
    payload = "&".join(f"{key}={value}" for key, value in sorted(params.items()) if value)
    return hashlib.sha1(f"{payload}{api_secret}".encode("utf-8")).hexdigest()


def multipart_form_data(fields: dict[str, str], file_field: str, file_path: Path) -> tuple[bytes, str]:
    boundary = f"----BabyOiBoundary{uuid.uuid4().hex}"
    chunks: list[bytes] = []

    for name, value in fields.items():
        chunks.append(f"--{boundary}\r\n".encode())
        chunks.append(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode())
        chunks.append(str(value).encode())
        chunks.append(b"\r\n")

    content_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
    chunks.append(f"--{boundary}\r\n".encode())
    chunks.append(
        (
            f'Content-Disposition: form-data; name="{file_field}"; filename="{file_path.name}"\r\n'
            f"Content-Type: {content_type}\r\n\r\n"
        ).encode()
    )
    chunks.append(file_path.read_bytes())
    chunks.append(b"\r\n")
    chunks.append(f"--{boundary}--\r\n".encode())
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def upload_image(config: CloudinaryConfig, image: FoodImage) -> str:
    timestamp = str(int(time.time()))
    upload_params = {
        "folder": config.folder,
        "overwrite": "true",
        "public_id": image.public_id,
        "timestamp": timestamp,
    }
    signature = sign_cloudinary_params(upload_params, config.api_secret)
    fields = {
        "api_key": config.api_key,
        "timestamp": timestamp,
        "folder": config.folder,
        "public_id": image.public_id,
        "overwrite": "true",
        "signature": signature,
    }
    body, content_type = multipart_form_data(fields, "file", image.path)
    request = urllib.request.Request(
        f"https://api.cloudinary.com/v1_1/{config.cloud_name}/image/upload",
        data=body,
        headers={"Content-Type": content_type},
        method="POST",
    )
    with urllib.request.urlopen(request, context=ssl.create_default_context(), timeout=60) as response:
        payload = json.loads(response.read().decode("utf-8"))

    secure_url = payload.get("secure_url")
    if not secure_url:
        raise RuntimeError(f"Cloudinary response did not contain secure_url for {image.path}")
    return secure_url


def sql_string(value: str) -> str:
    return value.replace("'", "''")


def write_sql(urls_by_food_id: dict[int, str], sql_path: Path) -> None:
    values = ",\n".join(
        f"        ({food_id}, '{sql_string(url)}')" for food_id, url in sorted(urls_by_food_id.items())
    )
    sql = f"""WITH food_image_seed(id, image_url) AS (
    VALUES
{values}
)
UPDATE "food_library" fl
SET "image_url" = s.image_url,
    "updated_at" = now(),
    "updated_by" = 'cloudinary-food-upload'
FROM food_image_seed s
WHERE fl."id" = s.id;
"""
    sql_path.parent.mkdir(parents=True, exist_ok=True)
    sql_path.write_text(sql, encoding="utf-8")


def update_database(db: DbConfig, urls_by_food_id: dict[int, str]) -> int:
    try:
        import psycopg
    except ImportError as error:
        raise SystemExit("Missing dependency: install with `python -m pip install psycopg[binary]`.") from error

    with psycopg.connect(
        host=db.host,
        port=db.port,
        dbname=db.database,
        user=db.username,
        password=db.password,
    ) as connection:
        with connection.cursor() as cursor:
            cursor.executemany(
                """
                UPDATE "food_library"
                SET "image_url" = %s,
                    "updated_at" = now(),
                    "updated_by" = 'cloudinary-food-upload'
                WHERE "id" = %s
                """,
                [(url, food_id) for food_id, url in sorted(urls_by_food_id.items())],
            )
            updated_count = cursor.rowcount
        connection.commit()
    return updated_count


def check_existing_food_ids(db: DbConfig, food_ids: Iterable[int]) -> set[int]:
    try:
        import psycopg
    except ImportError:
        return set()

    ids = list(food_ids)
    with psycopg.connect(
        host=db.host,
        port=db.port,
        dbname=db.database,
        user=db.username,
        password=db.password,
    ) as connection:
        with connection.cursor() as cursor:
            cursor.execute('SELECT "id" FROM "food_library" WHERE "id" = ANY(%s)', (ids,))
            return {int(row[0]) for row in cursor.fetchall()}


def run_pip_install() -> None:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "psycopg[binary]"])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Upload food_001..food_300 images to Cloudinary and update food_library.image_url."
    )
    parser.add_argument("--image-root", type=Path, default=DEFAULT_IMAGE_ROOT)
    parser.add_argument("--folder", default=None, help="Cloudinary folder. Default: food")
    parser.add_argument("--db-profile", default="quoc", help="Spring profile YAML to read DB config from. Default: quoc")
    parser.add_argument("--apply-db", action="store_true", help="Update the PostgreSQL database after upload.")
    parser.add_argument("--dry-run", action="store_true", help="Only scan files and print what would happen.")
    parser.add_argument("--install-db-driver", action="store_true", help="Install psycopg[binary] with pip before DB update.")
    parser.add_argument("--report-path", type=Path, default=DEFAULT_REPORT_PATH)
    parser.add_argument("--sql-path", type=Path, default=DEFAULT_SQL_PATH)
    parser.add_argument("--limit", type=int, default=None, help="Upload only the first N images for testing.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    images = discover_images(args.image_root)
    if args.limit is not None:
        images = images[: args.limit]

    found_ids = {item.food_id for item in images}
    missing_1_to_300 = [food_id for food_id in range(1, 301) if food_id not in found_ids]

    print(f"Found {len(images)} food image files in {args.image_root}")
    if missing_1_to_300:
        print(f"Missing IDs from 1..300: {missing_1_to_300}")

    db_config = load_db_config(args.db_profile)
    existing_ids = check_existing_food_ids(db_config, found_ids)
    if existing_ids:
        missing_in_db = sorted(found_ids - existing_ids)
        if missing_in_db:
            print(f"Image IDs not found in food_library: {missing_in_db}")

    if args.dry_run:
        for item in images[:10]:
            print(f"DRY RUN: food_id={item.food_id} file={item.path}")
        print("Dry run finished. No upload and no DB update.")
        return

    if args.install_db_driver:
        run_pip_install()

    cloudinary_config = load_cloudinary_config(args.folder)
    uploaded: dict[int, str] = {}
    failures: list[dict[str, str | int]] = []

    for index, image in enumerate(images, start=1):
        try:
            secure_url = upload_image(cloudinary_config, image)
            uploaded[image.food_id] = secure_url
            print(f"[{index}/{len(images)}] Uploaded food_id={image.food_id}: {secure_url}")
        except (urllib.error.URLError, RuntimeError, OSError) as error:
            failures.append({"food_id": image.food_id, "file": str(image.path), "error": str(error)})
            print(f"[{index}/{len(images)}] FAILED food_id={image.food_id}: {error}", file=sys.stderr)

    if uploaded:
        write_sql(uploaded, args.sql_path)
        print(f"Wrote SQL backup: {args.sql_path}")

    updated_count = 0
    if args.apply_db and uploaded:
        updated_count = update_database(db_config, uploaded)
        print(f"Updated database rows: {updated_count}")
    elif uploaded:
        print("DB not updated because --apply-db was not provided.")

    report = {
        "image_root": str(args.image_root),
        "cloudinary_folder": cloudinary_config.folder,
        "found_count": len(images),
        "uploaded_count": len(uploaded),
        "updated_db_rows": updated_count,
        "missing_1_to_300": missing_1_to_300,
        "uploaded": uploaded,
        "failures": failures,
    }
    args.report_path.parent.mkdir(parents=True, exist_ok=True)
    args.report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote report: {args.report_path}")

    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
