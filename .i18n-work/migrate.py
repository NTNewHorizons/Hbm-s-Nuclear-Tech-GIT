#!/usr/bin/env python3
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java"
LANG = ROOT / "src/main/resources/assets/hbm/lang/en_US.lang"

existing = {}
for raw in LANG.read_text(encoding="utf-8").splitlines():
    if "=" in raw and not raw.startswith("#"):
        key, value = raw.split("=", 1)
        existing[key] = value

generated = {}
obsolete = set()

def snake(value):
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", value)
    return re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")

def java_string(token):
    try:
        return json.loads(token)
    except Exception:
        return token[1:-1].replace(r'\"', '"').replace(r"\\", "\\")

def visible(value):
    if not re.search(r"[A-Za-z]{2,}", value): return False
    if re.fullmatch(r"[a-z0-9_.:/-]+", value): return False
    if value.startswith(("textures/", "hbm:", "tile.", "item.", "gui.", "commands.")): return False
    if "." in value and not value.endswith(".") and re.fullmatch(r"[A-Za-z0-9_.:/-]+", value): return False
    if re.fullmatch(r"(?:HE|TU|PU|mB|kg|HP|KyU|RAD|RAD/s|DPS|X|Y|Z|N|E|S|W)", value): return False
    return True

def key_for(kind, cls, value):
    words = snake(re.sub(r"§.", "", value))[:56].strip("_") or "text"
    base = f"{kind}.{snake(cls)}.{words}" if cls else f"{kind}.{words}"
    key = base
    index = 2
    while key in existing and existing[key] != value or key in generated and generated[key] != value:
        key = f"{base}_{index}"
        index += 1
    generated[key] = value
    return key

def split_plus(expr):
    parts, start, depth, quote, escape = [], 0, 0, False, False
    for i, char in enumerate(expr):
        if quote:
            if escape: escape = False
            elif char == "\\": escape = True
            elif char == '"': quote = False
        else:
            if char == '"': quote = True
            elif char in "([{" : depth += 1
            elif char in ")]}": depth -= 1
            elif char == "+" and depth == 0:
                parts.append(expr[start:i].strip())
                start = i + 1
    parts.append(expr[start:].strip())
    index = 0
    while index + 1 < len(parts):
        if re.fullmatch(r"\((?:byte|short|int|long|float|double)\)", parts[index]):
            parts[index:index + 2] = [parts[index] + " + " + parts[index + 1]]
        else:
            index += 1
    return parts

def localize_expr(expr, kind, cls, call):
    parts = split_plus(expr)
    if not any(re.fullmatch(r'"(?:\\.|[^"\\])*"', p) and visible(java_string(p)) for p in parts):
        return None
    value, args = "", []
    for part in parts:
        if re.fullmatch(r'"(?:\\.|[^"\\])*"', part):
            value += java_string(part).replace("%", "%%")
        else:
            value += "%s"
            args.append(part)
    key = key_for(kind, cls, value)
    suffix = ", " + ", ".join(args) if args else ""
    return f'{call}("{key}"{suffix})'

def matching(text, open_pos):
    depth, quote, escape = 0, False, False
    for i in range(open_pos, len(text)):
        char = text[i]
        if quote:
            if escape: escape = False
            elif char == "\\": escape = True
            elif char == '"': quote = False
        else:
            if char == '"': quote = True
            elif char == "(": depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0: return i
    return -1

def matching_delim(text, open_pos, opening, closing):
    depth, quote, escape = 0, False, False
    for i in range(open_pos, len(text)):
        char = text[i]
        if quote:
            if escape: escape = False
            elif char == "\\": escape = True
            elif char == '"': quote = False
        else:
            if char == '"': quote = True
            elif char == opening: depth += 1
            elif char == closing:
                depth -= 1
                if depth == 0: return i
    return -1

def replace_call(line, marker, kind, cls, call):
    pos = line.find(marker)
    if pos < 0: return line, False
    open_pos = line.find("(", pos)
    close_pos = matching(line, open_pos)
    if close_pos < 0: return line, False
    expr = line[open_pos + 1:close_pos]
    replacement = localize_expr(expr, kind, cls, call)
    if not replacement: return line, False
    return line[:open_pos + 1] + replacement + line[close_pos:], True

def repair_dropped_add(line):
    match = re.search(r'I18nUtil\.resolveKey\("(tooltip|overlay|gui)\.', line)
    if not match or any(token in line for token in (".add(", ".drawString(", "return ", "= I18nUtil")):
        return line
    start = match.start()
    if line[:start].strip() and not re.fullmatch(r"(?:if\s*\(.*\)\s*)?", line[:start].strip()):
        return line
    open_pos = line.find("(", start)
    close_pos = matching(line, open_pos)
    if close_pos < 0: return line
    collection = {"tooltip": "list", "overlay": "text", "gui": "lines"}[match.group(1)]
    return line[:start] + collection + ".add(" + line[start:close_pos + 1] + ")" + line[close_pos + 1:]

def add_import(text, statement):
    if statement in text: return text
    imports = list(re.finditer(r"^import .*?;\n", text, re.M))
    if imports:
        at = imports[-1].end()
        return text[:at] + statement + "\n" + text[at:]
    package = re.search(r"^package .*?;\n", text, re.M)
    return text[:package.end()] + "\n" + statement + "\n" + text[package.end():]

def localize_literals_in_calls(text, cls):
    markers = ("drawCustomInfoStat(", "drawCreativeTabHoveringText(", "drawLeftAligned(",
               "drawRightAligned(", ".drawString(", "drawStringWithShadow(", "drawCenteredString(",
               "displayTooltip(", "func_146283_a(", "addCommandHistory(", "list.add(", "text.add(", "lines.add(",
               "toolTip.add(", "info.add(", "label.add(", "tip.add(", "tty.add(",
               "new GuiButton(", "new FolderButton(", "setStackDisplayName(")
    changed = False
    for marker in markers:
        cursor = 0
        while True:
            pos = text.find(marker, cursor)
            if pos < 0: break
            open_pos = text.find("(", pos)
            close_pos = matching(text, open_pos)
            if close_pos < 0: break
            body = text[open_pos + 1:close_pos]
            kind = "gui" if not marker.startswith("setStack") else "item.name"
            def replace(match):
                nonlocal changed
                value = java_string(match.group(0))
                if not visible(value): return match.group(0)
                changed = True
                return f'I18nUtil.resolveKey("{key_for(kind, cls, value)}")'
            new_body = re.sub(r'"(?:\\.|[^"\\])*"', replace, body)
            text = text[:open_pos + 1] + new_body + text[close_pos:]
            cursor = open_pos + 1 + len(new_body)
    return text, changed

def localize_command_usage(text, cls):
    pattern = re.compile(r"(getCommandUsage\s*\([^)]*\)\s*\{.*?\breturn\s+)\"([^\"]+)\"(\s*;)", re.S)
    def replace(match):
        value = match.group(2)
        if value.startswith("commands."): return match.group(0)
        key = key_for("commands", cls.removeprefix("Command"), value)
        return match.group(1) + f'"{key}"' + match.group(3)
    return pattern.subn(replace, text)

def localize_display_setters(text):
    changed = False
    def replace(match):
        nonlocal changed
        method, token = match.group(1), match.group(2)
        value = java_string(token)
        if not visible(value): return match.group(0)
        kind = "item.missile_part.title" if method == "setTitle" else "item.missile_part.witty"
        changed = True
        return f'.{method}("{key_for(kind, "", value)}")'
    return re.sub(r'\.(setTitle|setWittyText)\(("(?:\\.|[^"\\])*")\)', replace, text), changed

def localize_gui_arrays(text, cls):
    cursor, changed = 0, False
    while True:
        pos = text.find("new String[]", cursor)
        if pos < 0: break
        open_pos = text.find("{", pos)
        if open_pos < 0: break
        close_pos = matching_delim(text, open_pos, "{", "}")
        if close_pos < 0: break
        body = text[open_pos + 1:close_pos]
        def replace(match):
            nonlocal changed
            value = java_string(match.group(0))
            if not visible(value): return match.group(0)
            changed = True
            return f'I18nUtil.resolveKey("{key_for("gui", cls, value)}")'
        new_body = re.sub(r'"(?:\\.|[^"\\])*"', replace, body)
        text = text[:open_pos + 1] + new_body + text[close_pos:]
        cursor = open_pos + len(new_body) + 1
    return text, changed

def localize_collectible_enum(path, text):
    specs = {
        "BlockSnowglobe.java": ((0, "label"), (1, "inscription")),
        "BlockPlushie.java": ((1, "inscription"),),
        "BlockBobble.java": ((2, "contribution"), (3, "inscription")),
        "ItemHolotapeImage.java": ((0, "color"), (2, "text")),
        "ItemCassette.java": ((0, "title"),),
    }
    if path.name not in specs: return text
    prefix = {"BlockSnowglobe.java": "snowglobe", "BlockPlushie.java": "plushie", "BlockBobble.java": "bobble",
              "ItemHolotapeImage.java": "holotape", "ItemCassette.java": "cassette"}[path.name]
    output = []
    for line in text.splitlines(keepends=True):
        constant = re.match(r"\s*([A-Z][A-Z0-9_]*)\s*\(", line)
        if not constant:
            output.append(line)
            continue
        tokens = list(re.finditer(r'"(?:\\.|[^"\\])*"', line))
        replacements = []
        for index, suffix in specs[path.name]:
            if index >= len(tokens): continue
            token = tokens[index]
            value = java_string(token.group(0))
            if not visible(value): continue
            key = f"{prefix}.{constant.group(1).lower()}.{suffix}"
            generated[key] = value
            replacements.append((token.start(), token.end(), f'"{key}"'))
        for start, end, replacement in reversed(replacements):
            line = line[:start] + replacement + line[end:]
        output.append(line)
    return "".join(output)

def localize_nei_names(path, text):
    if "/handler/nei/" not in path.as_posix(): return text, False
    key = f"nei.{snake(path.stem)}.name"
    changed, direct = False, False
    def replace_super(match):
        nonlocal changed
        value = java_string(match.group(1))
        if not visible(value): return match.group(0)
        generated[key] = value
        changed = True
        return f'super("{key}"'
    text = re.sub(r'super\(("(?:\\.|[^"\\])*")', replace_super, text)
    pattern = re.compile(r'(getRecipeName\s*\(\s*\)\s*\{\s*return\s+)"([^"\n]+)"(\s*;)', re.S)
    def replace_return(match):
        nonlocal changed, direct
        value = match.group(2)
        if not visible(value): return match.group(0)
        generated[key] = value
        changed = direct = True
        return match.group(1) + f'I18nUtil.resolveKey("{key}")' + match.group(3)
    text = pattern.sub(replace_return, text)
    return text, direct

def localize_rtg_facts(path, text):
    if path.name != "ItemRTGPellet.java": return text
    match = re.search(r'(private static final String\[\] facts\s*=\s*new String\[\]\s*\{)(.*?)(\};)', text, re.S)
    if not match: return text
    body = match.group(2)
    index = 0
    def replace(token):
        nonlocal index
        value = java_string(token.group(0))
        key = f"item.rtg.fact.{index}"
        index += 1
        generated[key] = value
        return f'"{key}"'
    body = re.sub(r'"(?:\\.|[^"\\])*"', replace, body)
    return text[:match.start(2)] + body + text[match.end(2):]

def localize_rbmk_names(path, text):
    if path.name != "ModItems.java": return text
    def replace(match):
        value = java_string(match.group(2))
        key = f"item.rbmk_fuel_name.{snake(value)}"
        generated[key] = value
        return match.group(1) + f'"{key}"'
    return re.sub(r'(new ItemRBMK(?:Pellet|Rod)\()((?:"(?:\\.|[^"\\])*") )?', lambda m: m.group(0), text) if False else re.sub(r'(new ItemRBMK(?:Pellet|Rod)\()("(?:\\.|[^"\\])*")', replace, text)

def process(path):
    source = path.read_text(encoding="utf-8")
    old = source
    try:
        base = subprocess.check_output(["git", "show", f"HEAD:{path.relative_to(ROOT)}"], cwd=ROOT).decode("utf-8", "replace")
        base_keys = re.findall(r'I18nUtil\.resolveKey\("([A-Za-z0-9_.:/-]+)"', base)
    except subprocess.CalledProcessError:
        base_keys = []
    def restore_nested(match):
        generated_key = match.group(1)
        original_key = existing.get(generated_key, "")
        prefix = f"gui.{snake(path.stem)}."
        suffix = generated_key[len(prefix):] if generated_key.startswith(prefix) else ""
        candidates = [key for key in base_keys if snake(key) == suffix]
        if not original_key and len(candidates) == 1:
            original_key = candidates[0]
        if "." not in original_key or not re.fullmatch(r"[A-Za-z0-9_.:/-]+", original_key):
            return match.group(0)
        obsolete.add(generated_key)
        return f'I18nUtil.resolveKey("{original_key}"'
    old = re.sub(r'I18nUtil\.resolveKey\(I18nUtil\.resolveKey\("([^"]+)"\)', restore_nested, old)
    cls = path.stem
    out, need_i18n, need_chat = [], False, False
    for line in old.splitlines(keepends=True):
        if "lines.add(I18nUtil.resolveKey" in line and not re.match(r"^\s*(?:(?:if|case)\b.*?)?lines\.add\(", line):
            call = line.find("lines.add(I18nUtil.resolveKey")
            line = line[:call] + line[call:].replace("lines.add(I18nUtil.resolveKey", "I18nUtil.resolveKey", 1)
            close = matching(line, line.find("(", call))
            if close >= 0 and line[close + 1:close + 2] == ")":
                line = line[:close + 1] + line[close + 2:]
        changed = False
        if "new ChatComponentText(" in line:
            pos = line.find("new ChatComponentText")
            open_pos = line.find("(", pos)
            close_pos = matching(line, open_pos)
            if close_pos >= 0:
                replacement = localize_expr(line[open_pos + 1:close_pos], "chat", cls, "new ChatComponentTranslation")
                if replacement:
                    line = line[:pos] + replacement + line[close_pos + 1:]
                    need_chat = changed = True
        for marker, kind, call in (("ChatBuilder.start(", "chat", "ChatBuilder.startTranslation"),
                                   ("new CommandException(", "commands", "new CommandException")):
            if marker in line:
                pos = line.find(marker)
                open_pos = line.find("(", pos)
                close_pos = matching(line, open_pos)
                if close_pos >= 0:
                    replacement = localize_expr(line[open_pos + 1:close_pos], kind, cls, call)
                    if replacement:
                        line = line[:pos] + replacement + line[close_pos + 1:]
                        changed = True
        for marker, kind in (("list.add(", "tooltip"), ("text.add(", "overlay"), ("lines.add(", "gui")):
            if marker in line:
                line, hit = replace_call(line, marker, kind, cls, "I18nUtil.resolveKey")
                need_i18n |= hit
                changed |= hit
                break
        if ".drawString(" in line:
            pos = line.find(".drawString(") + len(".drawString")
            close_pos = matching(line, pos)
            if close_pos >= 0:
                args = line[pos + 1:close_pos]
                first, depth, quote, escape = None, 0, False, False
                for i, char in enumerate(args):
                    if quote:
                        if escape: escape = False
                        elif char == "\\": escape = True
                        elif char == '"': quote = False
                    else:
                        if char == '"': quote = True
                        elif char == "(": depth += 1
                        elif char == ")": depth -= 1
                        elif char == "," and depth == 0:
                            first = i
                            break
                if first is not None:
                    replacement = localize_expr(args[:first], "gui", cls, "I18nUtil.resolveKey")
                    if replacement:
                        line = line[:pos + 1] + replacement + args[first:] + line[close_pos:]
                        need_i18n = changed = True
        if "getCommandUsage(" not in line and re.search(r"return\s+\"", line):
            # Command usage methods usually return on following line.
            pass
        out.append(line)
    text = "".join(out)
    text, call_changed = localize_literals_in_calls(text, cls)
    need_i18n |= call_changed
    text, _ = localize_display_setters(text)
    text = localize_collectible_enum(path, text)
    text, nei_i18n = localize_nei_names(path, text)
    need_i18n |= nei_i18n
    text = localize_rtg_facts(path, text)
    text = localize_rbmk_names(path, text)
    if "/inventory/gui/" in path.as_posix():
        text, array_changed = localize_gui_arrays(text, cls)
        need_i18n |= array_changed
    if "/commands/" in path.as_posix():
        text, _ = localize_command_usage(text, cls)
    if need_i18n: text = add_import(text, "import com.hbm.util.i18n.I18nUtil;")
    if need_chat:
        text = add_import(text, "import net.minecraft.util.ChatComponentTranslation;")
        if "ChatComponentText" not in text.replace("import net.minecraft.util.ChatComponentText;", ""):
            text = text.replace("import net.minecraft.util.ChatComponentText;\n", "")
    if text != source:
        path.write_text(text, encoding="utf-8")
        return 1
    return 0

changed = sum(process(path) for path in JAVA.rglob("*.java"))

fragment = ROOT / ".i18n-work/items.lang"
if fragment.exists():
    for raw in fragment.read_text(encoding="utf-8").splitlines():
        if "=" in raw:
            key, value = raw.split("=", 1)
            if key not in existing: generated[key] = value

if generated:
    with LANG.open("a", encoding="utf-8") as handle:
        handle.write("\n# Scripted hardcoded string localization\n")
        for key in sorted(generated): handle.write(f"{key}={generated[key]}\n")

if obsolete:
    lines = LANG.read_text(encoding="utf-8").splitlines()
    LANG.write_text("\n".join(line for line in lines if not any(line.startswith(key + "=") for key in obsolete)) + "\n", encoding="utf-8")

print(f"changed_files={changed} new_keys={len(generated)}")
