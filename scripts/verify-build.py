"""检查实际开发包的资源、依赖声明与测试隔离；不启动或修改其他模组。"""
from pathlib import Path
import hashlib
import json
import struct
import sys
import tomllib
import zipfile

project = Path(__file__).resolve().parents[1]
# 0.2.0 仅更新前置模组元数据，0.1.19 仍是编译基线。
friendswine_maven_range = "[0.1.19],[0.2.0]"
friendswine_fabric_versions = ["0.1.19", "0.2.0"]
source_metadata = project / "mod/src/main/resources/META-INF/neoforge.mods.toml"
description = tomllib.loads(source_metadata.read_text(encoding="utf-8"))["mods"][0]["description"].strip()
version = (project / "mod/build.gradle").read_text(encoding="utf-8").split("version = '", 1)[1].split("'", 1)[0]
jar = project / f"mod/build/libs/shaoxinpipaqu-{version}-NeoForge-1.21.1.jar"
with zipfile.ZipFile(jar) as archive:
    names = set(archive.namelist())
    assert not any("GameTests" in p or "/qa/" in p or p.startswith("data/shaoxinpipaqu/structure/") for p in names), "成品混入测试入口或夹具"
    assert not any(p.endswith((".lrc", ".srt")) or "/lyrics/" in p for p in names), "本版只做卡点，不打包完整歌词轴"
    metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
    assert metadata["mods"][0]["modId"] == "shaoxinpipaqu"
    assert metadata["mods"][0]["version"] == version
    assert metadata["mods"][0]["description"].strip() == description
    assert metadata["mods"][0]["authors"] == "jiaotan_"
    assert "com/shaoxinpipaqu/client/ClientConfig.class" in names, "本机卡点配置必须进入成品"
    for language in ("zh_cn", "en_us"):
        labels = json.loads(archive.read(f"assets/shaoxinpipaqu/lang/{language}.json"))
        for key in ("showCueText", "showMemePictures"):
            assert f"shaoxinpipaqu.config.{key}" in labels and f"shaoxinpipaqu.config.{key}.tooltip" in labels
    assert not any(p.startswith(("com/elfmcys/", "com/github/tartaricacid/")) for p in names), "成品不得嵌入可选模组"
    dependency = next(d for d in metadata["dependencies"]["shaoxinpipaqu"] if d["modId"] == "friendswine")
    assert dependency["versionRange"] == friendswine_maven_range and dependency["side"] == "BOTH"
    mixins = json.loads(archive.read("shaoxinpipaqu.mixins.json"))
    for name in mixins["mixins"] + mixins["client"]:
        assert f"com/shaoxinpipaqu/mixin/{name}.class" in names
    for path in names:
        if path.endswith(".json"):
            json.loads(archive.read(path))
    for item in ("wild_dog_milk", "forgetful_beef_noodles"):
        data = archive.read(f"assets/shaoxinpipaqu/textures/item/{item}.png")
        assert data[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", data[16:24]) == (128, 128)
        assert data[25] == 6, "图标必须为 RGBA"
    assert "assets/shaoxinpipaqu/sounds/pipa.ogg" in names
    for image, size in (("big_dog", (396, 388)), ("cabbage", (379, 512)), ("courage", (512, 512))):
        path = f"assets/shaoxinpipaqu/textures/meme/{image}.png"
        data = archive.read(path)
        assert data[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", data[16:24]) == size
        assert data[25] == 6 and data == (project / "mod/src/main/resources" / path).read_bytes(), "卡点纹理必须使用本次RGBA导出"
    for effect in ("forgetfulness", "great_grandma"):
        path = f"assets/shaoxinpipaqu/textures/mob_effect/{effect}.png"
        data = archive.read(path)
        assert data[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", data[16:24]) == (36, 36), "状态图标必须为36×36 PNG"
        assert data[25] == 6, "状态图标必须保留RGBA透明通道"
        assert data == (project / "mod/src/main/resources" / path).read_bytes(), "成品状态图标与当前导出资源不一致"
assert (project / "mod/gradle/wrapper/gradle-wrapper.jar").is_file()
assert hashlib.sha256((project / "assets/source/提取音频.m4a").read_bytes()).hexdigest() == "de7e938af75cb3d44820c8056d45387c00a95a14b8f4a02982a7979baccfee3a"
for image in json.loads((project / "assets/source/meme-references/selected-originals.json").read_text(encoding="utf-8-sig")):
    assert hashlib.sha256((project / "assets/source/meme-references" / image["file"]).read_bytes()).hexdigest() == image["sha256"], "选定原图不得被导出覆盖"
print(json.dumps({"jar": jar.name, "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(), "checks": "依赖、资源、JSON、RGBA、Mixin 类、测试隔离和音频原件通过"}, ensure_ascii=False))

if sys.argv[1:] == ["--matrix"]:
    targets = (
        ("Forge", "1.20.1", 61),
        ("Fabric", "1.20.1", 61),
        ("NeoForge", "1.21.1", 65),
        ("Fabric", "1.21.1", 65),
        ("NeoForge", "26.1.2", 69),
        ("Fabric", "26.1.2", 69),
    )
    shared_assets = project / "mod/src/main/resources/assets/shaoxinpipaqu"
    content = ["sounds/pipa.ogg", "textures/meme/big_dog.png", "textures/meme/cabbage.png",
               "textures/meme/courage.png", "textures/item/forgetful_beef_noodles.png",
               "textures/item/wild_dog_milk.png", "textures/mob_effect/forgetfulness.png",
               "textures/mob_effect/great_grandma.png"]
    for loader, minecraft, class_major in targets:
        module = (project / "mod" if (loader, minecraft) == ("NeoForge", "1.21.1")
                  else project / "ports" / f"{loader.lower()}-{minecraft}")
        # 一对端口可以单独修复并升版；历史 JAR 仍保留在各自的构建目录。
        settings = (project / "ports/shared-26.1.2/common.gradle" if minecraft == "26.1.2"
                    else module / "build.gradle")
        target_version = settings.read_text(encoding="utf-8").split("version = '", 1)[1].split("'", 1)[0]
        package = module / f"build/libs/shaoxinpipaqu-{target_version}-{loader}-{minecraft}.jar"
        with zipfile.ZipFile(package) as archive:
            names = set(archive.namelist())
            assert not any("/qa/" in name or "GameTests" in name for name in names), package.name
            actual_major = struct.unpack(">H", archive.read("com/shaoxinpipaqu/Pipa.class")[6:8])[0]
            assert actual_major == class_major, (package.name, actual_major, class_major)
            if loader == "Fabric":
                info = json.loads(archive.read("fabric.mod.json"))
                assert info["id"] == "shaoxinpipaqu" and info["version"] == target_version, package.name
                assert info["depends"]["friendswine"] == friendswine_fabric_versions and info["depends"]["minecraft"] == minecraft, package.name
                assert info["description"].strip() == description and info["authors"] == ["jiaotan_"], package.name
            else:
                metadata_file = "mods.toml" if loader == "Forge" else "neoforge.mods.toml"
                info = tomllib.loads(archive.read("META-INF/" + metadata_file).decode())
                assert info["mods"][0]["modId"] == "shaoxinpipaqu" and info["mods"][0]["version"] == target_version, package.name
                dependencies = {dep["modId"]: dep for dep in info["dependencies"]["shaoxinpipaqu"]}
                assert dependencies["friendswine"]["versionRange"] == friendswine_maven_range, package.name
                assert info["mods"][0]["description"].strip() == description and info["mods"][0]["authors"] == "jiaotan_", package.name
                assert dependencies["minecraft"]["versionRange"] == f"[{minecraft}]", package.name
            for asset in content:
                assert archive.read(f"assets/shaoxinpipaqu/{asset}") == (shared_assets / asset).read_bytes(), (package.name, asset)
            for file in (name for name in names if name.startswith("shaoxinpipaqu") and name.endswith("mixins.json")):
                config = json.loads(archive.read(file))
                prefix = config["package"].replace(".", "/")
                for mixin in config.get("mixins", []) + config.get("client", []):
                    assert prefix + "/" + mixin.replace(".", "/") + ".class" in names, (package.name, mixin)
        print(json.dumps({"jar": package.name, "sha256": hashlib.sha256(package.read_bytes()).hexdigest()}, ensure_ascii=False))
