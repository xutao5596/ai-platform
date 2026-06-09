# Agent 编码与文件处理强制规范

> **目的**:消除 Windows 容器 + PowerShell 5.1 + Git 三方编码不一致导致的灾难
> **适用范围**:所有 Agent、子会话、自动化脚本、CI
> **违反此规范 = 自动判 0 分,主线程拒绝合并**

---

## 1. 背景与根因

AI-Platform 在 Windows + PowerShell 5.1 + Git for Windows 环境下开发。三方编码处理不一致:
- Git 默认按系统代码页(中文 Windows = GBK/CP936)读文件
- PowerShell `Get-Content` / `Set-Content` / `Out-File` 默认带 UTF-8 **BOM** 或按系统代码页
- Java 编译器要求源文件是 **UTF-8(无 BOM)** 或显式 `-encoding UTF-8`
- Linux 容器 / macOS 默认 UTF-8

**踩过的坑**(2026-06-09 Sprint 3.1):
1. Agent 在容器里 `git show` 读 UTF-8 文件,Git 按 GBK 解码,中文变 mojibake,Agent 不知情直接写回 → 整个分支文件全部乱码
2. 主线程用 `Get-Content` → `Out-File` 反复转码,每次加 BOM,Java 编译报 "unmappable character"
3. 试图"逐行替换"乱码行,转码 2-3 轮后字节位移错位,文件彻底坏掉

---

## 2. 强制约束(违反 = 重做)

### 2.1 容器/Shell 启动时必须设置的环境变量

```bash
# Git 强制 UTF-8
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
git config --global core.quotepath false
git config --global i18n.commitencoding UTF-8
git config --global i18n.logoutputencoding UTF-8
```

PowerShell 等价:
```powershell
$env:LANG = "zh_CN.UTF-8"
$env:LC_ALL = "zh_CN.UTF-8"
git config --global core.quotepath false
```

### 2.2 文件读取 — **只能用 .NET 字节流 API**

```powershell
# 读 UTF-8 文件(带 BOM 也兼容)
$bytes = [System.IO.File]::ReadAllBytes($path)
$content = [System.Text.Encoding]::UTF8.GetString($bytes)

# 读 UTF-16 LE(罕见)
$content = [System.Text.Encoding]::Unicode.GetString($bytes)
```

**禁止使用**:
- `Get-Content` — 在中文文件上行为不可预测
- `cat` (Git Bash) — 同上

### 2.3 文件写入 — **必须 UTF-8 NO BOM**

```powershell
# 正确:UTF-8 无 BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding($False)
[System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
```

**禁止使用**:
- `Set-Content` — 默认带 BOM
- `Out-File` — 同上
- `>` 重定向 — 编码不可控

### 2.4 Git 操作

```bash
# 读分支文件
git --no-pager cat-file blob <hash> > /tmp/file.raw
# 然后用 .NET API 读

# 列出文件树
git --no-pager ls-tree <branch>:<path>
```

**禁止使用**:
- `git show <branch>:<file>` 直接管道给 PowerShell
- `git --no-pager show` 在 PowerShell 5.1 下

### 2.5 Java 源文件硬性要求

- 编码:UTF-8 **无 BOM**
- 换行:`\n` 或 `\r\n` 均可
- 编译命令: `mvn -o compile` (Maven 默认 UTF-8)

---

## 3. Agent 工作流强制步骤

每个 Agent 改完文件后,合并前**必须**执行:

```powershell
# 1. 检查改过的文件是否合法 UTF-8
$files = git diff --name-only develop
foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Host "ERROR: BOM found in $f" -ForegroundColor Red
    } else {
        Write-Host "OK: $f" -ForegroundColor Green
    }
}

# 2. 验证 Java 编译
cd ai-backend
mvn -o -pl <module> -am compile
# 必须是 BUILD SUCCESS
```

**主线程联调前再执行一次**:
```bash
cd ai-backend
mvn -o clean compile -DskipTests
# 必须 BUILD SUCCESS
```

---

## 4. 应急救援(如果发现文件已经被搞坏)

**不要尝试修补,直接重写**。

```powershell
# 1. 从 git 原始 blob 重新获取
$hash = "<git blob hash>"
git --no-pager cat-file blob $hash > C:\Temp\file.java
$bytes = [System.IO.File]::ReadAllBytes("C:\Temp\file.java")
if ($bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
    $content = [System.Text.Encoding]::Unicode.GetString($bytes)
} else {
    $content = [System.Text.Encoding]::UTF8.GetString($bytes)
}
$utf8 = New-Object System.Text.UTF8Encoding($False)
[System.IO.File]::WriteAllText($path, $content, $utf8)
```

或者**最简单**:整个文件用 `[string]` 单行重写,避免任何编码转换。

---

## 5. 速查表

| 操作 | 用 | 不用 |
|---|---|---|
| 读文件 | `[IO.File]::ReadAllBytes` + 显式 Encoding | `Get-Content` |
| 写文件 | `[IO.File]::WriteAllText(path, content, [UTF8Encoding]::new($false))` | `Set-Content` / `Out-File` |
| 检 BOM | 头 3 字节是否 `EF BB BF` | 眼睛看 |
| 修编码 | 从 git 重新取 + 整体重写 | 逐行替换 |
| Git 读文件 | `git cat-file blob <hash>` → .NET 读 | `git show` + 管道 |
| Java 编译 | `mvn -o compile` 看 BUILD SUCCESS | 手动 javac |

---

**记住**:编码问题处理原则 = **整体处理,字节流优先,显式 Encoding,绝不带 BOM**。
