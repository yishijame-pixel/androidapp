# 皮肤特效 Lottie 动画文件夹

把从 [LottieFiles.com](https://lottiefiles.com/) 下载的 JSON 文件放到这里。
SkinFx 会自动按 文件名 加载对应皮肤的动画。**找不到文件就自动回退**到手写 Compose 特效，不会崩溃。

## 文件名约定

| 皮肤 ID | 期望文件名 | LottieFiles 推荐搜索词 |
|---|---|---|
| `builtin::chiyan`     | `chiyan.json`     | `fire`, `flames`, `inferno`, `burning`, `campfire` |
| `builtin::jiyue`      | `jiyue.json`      | `lightning`, `thunder`, `electric strike` |
| `builtin::qingchuan`  | `qingchuan.json`  | `cherry blossom`, `sakura`, `falling petals` |
| `builtin::qingluan`   | `qingluan.json`   | `falling leaves`, `bamboo`, `forest particles` |
| `builtin::xinghe`     | `xinghe.json`     | `stars`, `meteor`, `shooting star`, `galaxy` |
| `builtin::hengwu`     | `hengwu.json`     | `gold dust`, `sparkles`, `magic particles` |

## 推荐流程
1. 打开 https://lottiefiles.com/
2. 在搜索框输入对应推荐关键词
3. 找一个**循环（loopable）**的、**透明背景**的、**主题色匹配**的动画
4. 点击 "Download Lottie JSON"
5. 重命名为对应的文件名（如 `chiyan.json`）
6. 放到这个文件夹（`app/src/main/assets/skin_fx/`）
7. 不需要修改任何代码，重新跑 App 即可

## 推荐的几个具体动画
- **火焰**：https://lottiefiles.com/animations/fire-loop-... （搜 "fire loop"）
- **闪电**：https://lottiefiles.com/animations/lightning-... （搜 "lightning strike loop"）
- **樱花**：https://lottiefiles.com/animations/falling-petals-... （搜 "sakura"）

## 调试
找不到 JSON 时 SkinFx 会自动用之前的手写 Compose 特效兜底，所以即使一个文件都不放，
App 也不会崩溃，只是特效是手写版本而不是 Lottie 版本。
