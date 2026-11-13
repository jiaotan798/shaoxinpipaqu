"""读取 1.20.1 实际图片顶点，验证相机与世界视图合成后的纹理方向；无需启动游戏。"""
from math import cos, radians, sin
from pathlib import Path
import re


def rotate(point, axis, angle):
    """按右手坐标系旋转，用于复现原版相机及视图矩阵的顺序。"""
    x, y, z = point
    c, s = cos(angle), sin(angle)
    if axis == "x":
        return x, c * y - s * z, s * y + c * z
    if axis == "y":
        return c * x + s * z, y, -s * x + c * z
    return c * x - s * y, s * x + c * y, z


def screen(point, yaw, pitch, tilt):
    """复现 1.20.1 的视图 Rx(pitch)Ry(yaw+π) 与相机 Ry(-yaw)Rx(pitch)。"""
    point = rotate(point, "z", radians(tilt))
    point = rotate(point, "x", radians(pitch))
    point = rotate(point, "y", radians(-yaw))
    point = rotate(point, "y", radians(yaw + 180))
    return rotate(point, "x", radians(pitch))


def check(vertices):
    """检查原图左右边和上下边在屏幕的顺序，覆盖三图宽高比及单图、双图倾角。"""
    cases = 0
    for aspect in (396 / 388, 379 / 512, 1):
        for yaw in (0, 90, 180, 270):
            for pitch in (-60, 0, 60):
                for tilt in (0, 15, -15):
                    edges = {"left": [], "right": [], "top": [], "bottom": []}
                    for x, y, u, v in vertices:
                        sx, sy, _ = screen((x * 0.9 * aspect, y * 0.9, 0), yaw, pitch, tilt)
                        edges["left" if u == 0 else "right"].append(sx)
                        edges["top" if v == 0 else "bottom"].append(sy)
                    assert sum(edges["left"]) < sum(edges["right"]), "图片左右镜像"
                    assert sum(edges["top"]) > sum(edges["bottom"]), "图片上下颠倒"
                    cases += 1
    return cases


project = Path(__file__).resolve().parents[1]
source = project / "ports/shared-1.20.1/src/main/java/com/shaoxinpipaqu/client/MemeEffects.java"
body = source.read_text(encoding="utf-8").split("private static void drawPicture(", 1)[1].split("private static void vertex(", 1)[0]
matches = re.findall(r"vertex\(out,transform,(-?halfWidth),(-?halfHeight),([01]),([01]),0xFFFFFF,alpha\)", body)
assert len(matches) == 4, "必须检查实际图片四个顶点，渲染写法改变时同步更新检查"
vertices = [( -1 if x.startswith("-") else 1, -1 if y.startswith("-") else 1, int(u), int(v)) for x, y, u, v in matches]
cases = check(vertices)
# 旧实现使用相反的 U；确认断言确实能识别 issue #1，而非只证明当前代码能执行。
try:
    check([(x, y, 1 - u, v) for x, y, u, v in vertices])
except AssertionError as failure:
    assert str(failure) == "图片左右镜像"
else:
    raise AssertionError("回归检查未识别旧版镜像映射")
print(f"PASS：1.20.1 共用图片渲染 {cases} 个方向组合，旧版镜像映射已被拒绝；不替代客户端验收。")
