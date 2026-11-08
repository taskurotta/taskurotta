import os
import sys
import re
from pathlib import Path

import yaml

def get_mandatory_headers_from_template(template_path):
    """Extract mandatory headers from a template file.
    Headers are defined as '## Header Name | Alternative Name'
    """
    headers = []
    try:
        with open(template_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line.startswith("## "):
                    variants = [v.strip() for v in line[3:].split('|')]
                    headers.append(tuple(variants))
    except FileNotFoundError:
        return None
    return headers

def lint_markdown_file(file_path, mandatory_headers_map):
    """Basic linter for Living Documentation files."""
    errors = []
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except (FileNotFoundError, UnicodeDecodeError):
        return []

    # Check for emojis (forbidden by project rules)
    emoji_pattern = r'[\U00010000-\U0010ffff]|[✀-➿]|[☀-⛿]'
    if re.search(emoji_pattern, content):
        errors.append("Forbidden characters (potentially emojis) detected.")

    # Determine which template to use for this file
    template_type = None
    if "adr/" in file_path:
        template_type = "adr"
    elif "domains/" in file_path:
        if "domain.md" in file_path:
            template_type = "domain"
        elif "components/" in file_path:
            template_type = "component"
        else:
            template_type = None
    elif "howto/" in file_path and file_path.endswith("playbook.md"):
        template_type = "howto"

    if template_type and template_type in mandatory_headers_map:
        variants_list = mandatory_headers_map[template_type]
        for variants in variants_list:
            found = False
            for v in variants:
                if f"## {v}" in content:
                    found = True
                    break
            if not found:
                primary_name = variants[0]
                errors.append(f"Missing mandatory section: {primary_name} (or its aliases)")

    return errors

def audit_structural_links():
    """
    Verifies structural integrity of documentation levels (L1 -> L2 -> L3)
    and Epic linkage.
    """
    errors = []
    domains_root = 'docs/domains'
    readme_path = 'docs/README.md'
    epics_dir = 'docs/epics'

    if not os.path.exists(domains_root):
        return ["Domains root directory not found."]

    # 1. Check L1 (README) vs L2 (Filesystem)
    if os.path.exists(readme_path):
        with open(readme_path, 'r', encoding='utf-8') as f:
            content = f.read()
            # Extract domain links: [Name](domains/name/domain.md)
            readme_domains = set(re.findall(r'\[.*?\]\(domains/([\w-]+)/domain\.md\)', content))

        fs_domains = set([d for d in os.listdir(domains_root) if os.path.isdir(os.path.join(domains_root, d))])

        missing_in_fs = readme_domains - fs_domains
        missing_in_readme = fs_domains - readme_domains

        for d in missing_in_fs:
            errors.append(f"L1->L2: Domain {d} mentioned in README but missing in filesystem")
        for d in missing_in_readme:
            errors.append(f"L2->L1: Domain {d} exists in filesystem but missing in README")
    else:
        errors.append("L1: docs/README.md not found")

    # 2. Check L2 (domain.md) vs L3 (components/*.md)
    for domain in os.listdir(domains_root):
        domain_dir = os.path.join(domains_root, domain)
        if not os.path.isdir(domain_dir):
            continue

        domain_file = os.path.join(domain_dir, 'domain.md')
        components_dir = os.path.join(domain_dir, 'components')

        if not os.path.exists(domain_file):
            errors.append(f"L2: Domain {domain} is missing domain.md")
            continue

        if os.path.exists(components_dir) and os.path.isdir(components_dir):
            with open(domain_file, 'r', encoding='utf-8') as f:
                domain_content = f.read()

            components = [f for f in os.listdir(components_dir) if f.endswith('.md')]
            for comp in components:
                comp_name = comp[:-3] # remove .md
                if comp_name not in domain_content and comp not in domain_content:
                    errors.append(f"L3->L2: Component {comp} in {domain} is NOT mentioned in domain.md")

    # 3. Check Epic Linkage
    if os.path.exists(epics_dir):
        epics = [f for f in os.listdir(epics_dir) if f.endswith('.md')]
        for epic in epics:
            found = False
            for root, dirs, files in os.walk(domains_root):
                for file in files:
                    if file.endswith('.md'):
                        try:
                            with open(os.path.join(root, file), 'r', encoding='utf-8', errors='ignore') as f:
                                if epic in f.read():
                                    found = True
                                    break
                        except Exception:
                            continue
                if found: break
            if not found:
                errors.append(f"Epic: {epic} is NOT referenced in any domain documentation")

    return errors

def audit_howto():
    """
    Phase: How-to playbooks.
    1. Each docs/howto/<slug>/ directory must contain playbook.md.
    2. L1 catalog: every playbook must be listed in docs/README.md.
    3. Inverse index cross-check: docs/howto/README.md (domain -> playbooks)
       must equal the union of the playbooks' "Зависимости (домены/компоненты)" sections.
    """
    errors = []
    howto_root = 'docs/howto'
    readme_path = 'docs/README.md'
    index_path = os.path.join(howto_root, 'README.md')

    if not os.path.exists(howto_root):
        return errors

    playbooks = sorted([d for d in os.listdir(howto_root)
                        if os.path.isdir(os.path.join(howto_root, d))])

    # 1. playbook.md per directory
    for pb in playbooks:
        if not os.path.exists(os.path.join(howto_root, pb, 'playbook.md')):
            errors.append(f"Howto: {pb}/ is missing playbook.md")

    if not playbooks:
        return errors

    # 2. L1 catalog
    if os.path.exists(readme_path):
        with open(readme_path, 'r', encoding='utf-8') as f:
            readme_content = f.read()
        for pb in playbooks:
            if pb not in readme_content:
                errors.append(f"Howto->L1: Playbook {pb} exists in filesystem but missing in README catalog")
    else:
        errors.append("Howto: docs/README.md not found")

    # 3. Inverse index cross-check
    index_pairs = set()
    if not os.path.exists(index_path):
        errors.append("Howto: docs/howto/README.md (inverse index) is missing")
    else:
        with open(index_path, 'r', encoding='utf-8') as f:
            index_content = f.read()
        for line in index_content.splitlines():
            if not line.strip().startswith('|'):
                continue
            cells = [c.strip() for c in line.strip().strip('|').split('|')]
            if len(cells) < 2:
                continue
            domain = cells[0]
            if not re.match(r'^[a-z0-9-]+$', domain):
                continue  # header / separator rows
            for slug in re.findall(r'\[[^\]]+\]\(([^)\s]+?)/playbook\.md\)', cells[1]):
                index_pairs.add((domain, slug))

    declared_pairs = set()
    for pb in playbooks:
        pb_path = os.path.join(howto_root, pb, 'playbook.md')
        if not os.path.exists(pb_path):
            continue
        with open(pb_path, 'r', encoding='utf-8') as f:
            lines = f.read().splitlines()
        in_section = False
        for line in lines:
            if line.startswith('## '):
                in_section = line.strip().startswith('## Зависимости')
                continue
            if in_section:
                for domain in re.findall(r'\.\./domains/([a-z0-9-]+)/domain\.md', line):
                    declared_pairs.add((domain, pb))

    for domain, pb in sorted(declared_pairs - index_pairs):
        errors.append(f"Howto index: playbook {pb} depends on domain {domain}, but docs/howto/README.md does not list it")
    for domain, pb in sorted(index_pairs - declared_pairs):
        errors.append(f"Howto index: docs/howto/README.md lists playbook {pb} under domain {domain}, but the playbook's 'Зависимости (домены/компоненты)' does not declare it")

    return errors


def lint_adrs():
    """Phase: ADR YAML frontmatter (mandatory fields, valid status, supersedes cross-references)."""
    errors_by_file = {}
    adr_dir = Path("docs/adr")
    if not adr_dir.exists():
        return errors_by_file

    required_fields = {"id", "title", "status", "date"}
    valid_statuses = {"Draft", "Accepted", "Superseded", "Deprecated"}
    known_ids = {}
    adr_files = sorted(adr_dir.glob("[0-9][0-9][0-9][0-9]*.md"))

    for path in adr_files:
        content = path.read_text(encoding="utf-8")
        if not content.startswith("---"):
            errors_by_file.setdefault(str(path), []).append("Missing YAML Frontmatter ('---' at the beginning of the file)")
            continue

        parts = content.split("---", 2)
        if len(parts) < 3:
            errors_by_file.setdefault(str(path), []).append("YAML Frontmatter not closed correctly")
            continue

        try:
            meta = yaml.safe_load(parts[1])
        except yaml.YAMLError as e:
            errors_by_file.setdefault(str(path), []).append(f"YAML parsing error: {e}")
            continue

        if not isinstance(meta, dict):
            errors_by_file.setdefault(str(path), []).append("YAML Frontmatter is not a mapping")
            continue

        missing = required_fields - meta.keys()
        if missing:
            errors_by_file.setdefault(str(path), []).append(f"Missing mandatory YAML fields: {sorted(missing)}")

        status = meta.get("status")
        if status not in valid_statuses:
            errors_by_file.setdefault(str(path), []).append(f"Invalid status '{status}'. Allowed: {sorted(valid_statuses)}")

        adr_id = meta.get("id")
        if adr_id:
            known_ids[adr_id] = (path, meta)

    for adr_id, (path, meta) in known_ids.items():
        supersedes = meta.get("supersedes")
        if supersedes:
            if supersedes not in known_ids:
                errors_by_file.setdefault(str(path), []).append(f"References non-existent ADR: '{supersedes}'")
            else:
                target_path, target_meta = known_ids[supersedes]
                if target_meta.get("status") != "Superseded":
                    errors_by_file.setdefault(str(path), []).append(
                        f"Supersedes '{supersedes}', but {target_path.name} has status '{target_meta.get('status')}' instead of 'Superseded'")

    return errors_by_file


def lint_public_docs():
    """Phase: docs/public copies (emojis, LaTeX symbols, broken internal links and static assets)."""
    errors_by_file = {}
    public_dir = Path("docs/public")
    if not public_dir.exists():
        return errors_by_file
    static_dir = public_dir / "public"

    emoji_pattern = re.compile(r'[\U00010000-\U0010ffff]|[✀-➿]|[☀-⛿]')
    latex_pattern = re.compile(r'\$.*?\$')

    for file_path in sorted(public_dir.rglob('*.md')):
        try:
            content = file_path.read_text(encoding='utf-8')
        except (OSError, UnicodeDecodeError):
            continue

        errs = []
        for i, line in enumerate(content.splitlines(), 1):
            if emoji_pattern.search(line):
                errs.append(f"Line {i}: Forbidden characters (potentially emojis) detected.")
            if latex_pattern.search(line):
                errs.append(f"Line {i}: LaTeX symbol detected (replace with Unicode or plain text).")

        base_dir = file_path.parent
        for i, line_text in enumerate(content.splitlines(), 1):
            for link in re.findall(r'\[.*?\]\((.*?)\)', line_text):
                if link.startswith('#') or link.startswith('http'):
                    continue
                if link.startswith('/'):
                    if not (static_dir / link.lstrip('/')).exists():
                        errs.append(f"Line {i}: Broken static link: {link} (file not found in docs/public/public/)")
                    continue
                if not (base_dir / link).resolve().exists():
                    errs.append(f"Line {i}: Broken internal link: {link}")

        if errs:
            errors_by_file[str(file_path)] = errs

    return errors_by_file


def lint_internal_md_links():
    """Phase: resolvability of relative markdown links in internal SSoT docs
    (docs/domains, docs/howto, docs/system, docs/adr, docs/epics, guidelines, README).
    docs/public has its own phase (lint_public_docs); templates are excluded
    (placeholder links). Wiki-style [[...]] links are not markdown links —
    a separate issue.
    """
    errors_by_file = {}
    docs_root = Path("docs")
    for file_path in sorted(docs_root.rglob('*.md')):
        rel = str(file_path)
        if 'docs/public/' in rel or 'templates/' in rel:
            continue
        try:
            content = file_path.read_text(encoding='utf-8')
        except (OSError, UnicodeDecodeError):
            continue

        errs = []
        base_dir = file_path.parent
        for i, line_text in enumerate(content.splitlines(), 1):
            for link in re.findall(r'\[[^\]]*\]\((.*?)\)', line_text):
                link = link.strip()
                if link.startswith('#') or link.startswith(('http://', 'https://', 'mailto:', '/')):
                    continue
                target = link.split('#')[0].split('"')[0].strip()
                if not target:
                    continue
                if not (base_dir / target).resolve().exists():
                    errs.append(f"Line {i}: Broken internal link: {link} (file not found)")

        if errs:
            errors_by_file[str(file_path)] = errs

    return errors_by_file


def main():
    docs_dir = "docs"
    template_dir = "docs/templates"

    # Map template files to their internal type identifier
    template_map = {
        "adr.md": "adr",
        "domain.md": "domain",
        "component.md": "component",
        "howto.md": "howto",
    }

    mandatory_headers_map = {}
    for template_file, type_id in template_map.items():
        path = os.path.join(template_dir, template_file)
        headers = get_mandatory_headers_from_template(path)
        if headers:
            mandatory_headers_map[type_id] = headers
        else:
            print(f"Warning: Could not load mandatory headers from {path}", file=sys.stderr)

    all_errors = {}

    if not os.path.exists(docs_dir):
        print(f"Error: {docs_dir} directory not found.")
        sys.exit(1)

    # 1. File-by-file linting
    for root, dirs, files in os.walk(docs_dir):
        for file in files:
            if file.endswith(".md"):
                if "templates/" in root:
                    continue
                full_path = os.path.join(root, file)
                errors = lint_markdown_file(full_path, mandatory_headers_map)
                if errors:
                    all_errors[full_path] = errors

    # 2. Structural audit (+ howto: playbook dirs, L1 catalog, inverse index)
    structural_errors = audit_structural_links() + audit_howto()
    if structural_errors:
        all_errors["STRUCTURAL_INTEGRITY"] = structural_errors

    # 3. ADR frontmatter audit
    for file_path, errs in lint_adrs().items():
        all_errors.setdefault(file_path, []).extend(errs)

    # 4. Public docs audit
    for file_path, errs in lint_public_docs().items():
        all_errors.setdefault(file_path, []).extend(errs)

    # 5. Internal SSoT docs: markdown link resolvability
    for file_path, errs in lint_internal_md_links().items():
        all_errors.setdefault(file_path, []).extend(errs)

    if all_errors:
        for file, errs in all_errors.items():
            print(f"FILE/SCOPE: {file}")
            for e in errs:
                print(f"  - {e}")
        sys.exit(1)
    else:
        print("All documentation passes architectural style and structural checks.")
        sys.exit(0)

if __name__ == "__main__":
    main()
