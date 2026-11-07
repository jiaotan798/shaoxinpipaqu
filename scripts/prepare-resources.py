"""从保留原件生成游戏资源，并记录精确 PCM 循环时钟；不改音频与透明图源文件。"""
from pathlib import Path
import hashlib
import json
import os
import struct
import subprocess
import tempfile
import wave
import zlib

PROJECT = Path(__file__).resolve().parents[1]
FFMPEG = Path(os.environ.get("FFMPEG", "ffmpeg"))
RES = PROJECT / "mod/src/main/resources/assets/shaoxinpipaqu"


def run(*args):
    subprocess.run([str(FFMPEG), "-hide_banner", "-loglevel", "error", "-y", *map(str, args)], check=True)


def icon(path, rgb):
    """仅为内部 airborne 姿势标记生成菱形，不覆盖两个玩家可见效果的设计图。"""
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    rows = b"".join(b"\0" + b"".join(bytes((*rgb, 255 if abs(x-7.5)+abs(y-7.5)<7.5 else 0)) for x in range(16)) for y in range(16))
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB",16,16,8,6,0,0,0)) + chunk(b"IDAT",zlib.compress(rows)) + chunk(b"IEND",b""))


def prepare_effect_icons():
    """从批准的概念原图导出36像素状态图标，原图保留供后续修改。"""
    for name, image in (("forgetfulness", "forgetfulness-concept.png"), ("great_grandma", "great-grandma-concept.png")):
        # 18像素会丢失幽灵双眼；36像素配合最近邻保留五官，透明度二值化清除边缘杂点。
        run("-i", PROJECT / "assets/source/effect-icons/v1" / image,
            "-vf", "scale=36:36:flags=neighbor,format=rgba,lut=a='if(gte(val,128),255,0)'",
            "-frames:v", 1, RES / f"textures/mob_effect/{name}.png")


def prepare_meme_textures():
    """从用户选定原件导出限512像素的RGBA纹理；保留背景和文案，原件字节不变。"""
    destination = RES / "textures/meme"
    destination.mkdir(parents=True, exist_ok=True)
    for name, source in (("big_dog", "big-dog-bark-original.jpg"),
                         ("cabbage", "smiling-cabbage-original.jpg"),
                         ("courage", "chubby-courage-original.png")):
        original = PROJECT / "assets/source/meme-references" / source
        digest = hashlib.sha256(original.read_bytes()).hexdigest()
        # 不放大已有小图；按比例压缩大白菜图，减小GPU纹理占用，不做裁剪或AI重绘。
        run("-i", original, "-vf", "scale='min(512,iw)':'min(512,ih)':force_original_aspect_ratio=decrease,format=rgba",
            "-frames:v", 1, destination / f"{name}.png")
        assert hashlib.sha256(original.read_bytes()).hexdigest() == digest


def main():
    source = PROJECT / "assets/source/提取音频.m4a"
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    with tempfile.TemporaryDirectory(prefix="pipa-") as folder:
        pcm = Path(folder) / "pipa.wav"
        # 转成 16 位 PCM 前衰减 14.3 dB，使琵琶曲接近《朋友的酒》0.1.19 的 -24.5 LUFS。
        run("-i", source, "-af", "volume=-14.3dB", "-ac", 1, "-ar", 44100, "-c:a", "pcm_s16le", pcm)
        with wave.open(str(pcm), "rb") as audio:
            frames, rate = audio.getnframes(), audio.getframerate()
        duration = frames / rate
        # 两端各 20 毫秒淡化，避免循环接缝突跳；原件完整保留，游戏导出时长不变。
        run("-i", pcm, "-af", f"afade=t=in:d=0.02,afade=t=out:st={duration-0.02}:d=0.02", "-c:a", "libvorbis", "-q:a", 5, RES / "sounds/pipa.ogg")
        run("-i", RES / "sounds/pipa.ogg", "-c:a", "pcm_s16le", Path(folder) / "verify.wav")
        with wave.open(str(Path(folder) / "verify.wav"), "rb") as audio:
            assert audio.getnframes() == frames, "OGG 导出改变了循环长度"
    java = PROJECT / "mod/src/main/java/com/shaoxinpipaqu/AudioInfo.java"
    java.write_text(f'''package com.shaoxinpipaqu;
/** 由 scripts/prepare-resources.py 生成；修改音频请重跑源脚本，不手动改时钟。 */
public final class AudioInfo {{
    public static final int SAMPLE_RATE = {rate};
    public static final long FRAMES = {frames}L;
    public static final double LOOP_TICKS = FRAMES * 20.0 / SAMPLE_RATE;
    private AudioInfo() {{}}
}}
''', encoding="utf-8")
    for item, image in (("wild_dog_milk", "wild-dog-milk-cutout-ai.png"), ("forgetful_beef_noodles", "beef-noodles-cutout-ai.png")):
        run("-i", PROJECT / "assets/source/food-references" / image, "-vf", "scale=128:128:force_original_aspect_ratio=decrease,pad=128:128:(ow-iw)/2:(oh-ih)/2:color=black@0", "-frames:v", 1, RES / f"textures/item/{item}.png")
        (RES / f"models/item/{item}.json").write_text(json.dumps({"parent":"minecraft:item/generated","textures":{"layer0":f"shaoxinpipaqu:item/{item}"}}),encoding="utf-8")
    prepare_effect_icons()
    prepare_meme_textures()
    icon(RES / "textures/mob_effect/airborne.png", (192,239,255))
    assert hashlib.sha256(source.read_bytes()).hexdigest() == digest
    print(json.dumps({"source_sha256":digest,"frames":frames,"sample_rate":rate,"seconds":duration},ensure_ascii=False))


if __name__ == "__main__":
    main()
