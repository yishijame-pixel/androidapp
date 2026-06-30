# FunLife SuperTux Fork（ST-FORK）

> **产品决策：** 已确认（2026-06-26）— **Fork 经典模式 = FunLife 横版 1:1 主线**  
> **改编 Kotlin bundle = 可选 fallback**  
> **战略文档：** [`docs/supertux-fork-strategy.md`](../../docs/supertux-fork-strategy.md)  
> **产品路线图：** [`docs/supertux-funlife-product-roadmap.md`](../../docs/supertux-funlife-product-roadmap.md)

本目录为 FunLife 维护的 SuperTux fork 根目录（与 upstream 同构：`src/`、`data/`、`CMakeLists.txt` …）。

## 同步 upstream

```powershell
powershell -File backend/tools/bootstrap_supertux_fork.ps1
```

源：`reference-assets/supertux`（pin 见 `source_pin.json`）  
保留：`patches/`、`NOTICE`、`source_pin.json`、`FUNLIFE.md`、`.gitignore`

## 构建（Phase 1 PC）

见 [`docs/supertux-fork-build-guide.md`](../../docs/supertux-fork-build-guide.md)。

## Patch 策略

见 [`patches/README.md`](patches/README.md)。
