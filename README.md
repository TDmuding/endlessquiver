# 无限箭袋（Endless Quiver）

![封面](cover/cover_github_1280x640.png)

Minecraft **1.20.1 Forge** 模组，以 **Curios API** 为前置。

一个饰品：**无限箭袋**。它只有一个「记录格」，用来记住**一种**箭矢（原版箭、药水箭，或者模组箭矢）：
把箭矢拿给箭袋看一眼就会记下这一种箭，**箭矢本身原样退回背包**，不会被收走。
把它装备到 Curios 的 **背饰（back）** 栏位，射箭时就会无限供应记录的那种箭，**不消耗**手中或背包里的箭。

---

## 一、功能一览

| 功能 | 说明 |
| --- | --- |
| 无限箭矢 | 装备在背饰栏，弓/弩自动从箭袋取箭，取的是副本，箭袋里的箭数量永不减少 |
| 只记录不收取 | 箭袋只记「哪一种箭」，不存实物；界面里显示的那一支是**预览**，取不出来，也不会被消耗 |
| 手持右键打开 | **手持**箭袋，**右键**即可打开记录界面（对着空气或对着方块都可以，不需要潜行） |
| 只认箭类 | 记录格只认 `ArrowItem` 的子类（原版箭、药水箭都行），其他物品不会被记录 |
| 射出去捡不回 | 从箭袋射出的箭落地后**不能拾取**，不会把无限箭回收进背包刷物品 |
| 跨模组弓 | 走 Forge 的 `LivingGetProjectileEvent`，任何使用原版取弹药流程的模组弓/弩都能读到箭袋里的箭 |

### 使用流程

1. 合成 / 拿到「无限箭袋」，先拿在手里。
2. **右键**打开记录界面（不用潜行），把想无限使用的箭矢放上去（例如一支「治疗之箭」）——箭袋记下这一种箭，**箭矢随即退回背包**（也可以 `shift + 点击` 背包里的箭直接记录）。
3. 把箭袋装备到 Curios 的**背饰**栏（默认按 `G` 或点击饰品栏图标打开饰品界面）。
4. 拿出任意弓/弩正常射击 —— 射出的永远是箭袋记录的那一种箭，背包里的箭一支都不会少。

> 提示：箭袋在背包里但**没有装备**时，只起「记录/查看」的作用，不会供箭。
> 反过来，装备在背饰栏但**还没记录箭**时，也不会供箭，一切照原版走。

### 合成配方

工作台 3×3：**回响碎片占四角**，**顶上中间放一个下界之星**，**正中间放末影之眼**，
**末影之眼左右两格放下界合金锭**，**最下一行中间放下界合金块**。

| | | |
| --- | --- | --- |
| 回响碎片 | **下界之星** | 回响碎片 |
| **下界合金锭** | **末影之眼** | **下界合金锭** |
| 回响碎片 | **下界合金块** | 回响碎片 |

- 回响碎片 = `minecraft:echo_shard`（4 个，四角）
- 下界之星 = `minecraft:nether_star`（1 个）
- 末影之眼 = `minecraft:ender_eye`（1 个）
- 下界合金锭 = `minecraft:netherite_ingot`（2 个）
- 下界合金块 = `minecraft:netherite_block`（1 个）

配方文件：`src/main/resources/data/endlessquiver/recipes/quiver.json`；
另配了一条进度 `data/endlessquiver/advancements/recipes/quiver.json`（拿到回响碎片后，配方会出现在配方书里）。

---

## 二、安装

1. 安装 **Minecraft Forge 1.20.1**（47.x）。
2. 把 **Curios API**（`curios-forge-5.14.1+1.20.1.jar` 或更高的 5.x）放进 `mods/`。
3. 把编译出的 `endlessquiver-1.0.0.jar` 放进 `mods/`。

---

## 三、实现要点（给想改代码的人）

### 1. 无限供箭：`LivingGetProjectileEvent`

弓、弩发射时都会调用 `Player#getProjectile(ItemStack)` 去拿弹药。Forge 在这个方法的每个返回分支上都套了
`ForgeHooks.getProjectile(...)`，而 `ForgeHooks.getProjectile` 内部只做一件事：发一个
`LivingGetProjectileEvent`，然后返回 `event.getProjectileItemStack()`。

因为这是**所有武器取弹药的唯一汇聚点**，我们只要监听它、把玩家装备着的箭袋里的箭**换成副本**塞回去即可：

```java
ItemStack arrow = QuiverItem.getEquippedArrow(player);   // 从 NBT 读出箭矢（已经是副本）
if (arrow.isEmpty()) return;
event.setProjectileItemStack(arrow.copy());              // 再复制一次，双保险
```

为什么是“无限”的：拿到弹药之后弓会执行 `ItemStack#shrink(1)`，被消耗掉的是我们返回的**副本**，
玩家背包和箭袋里的原始物品一个字节都不会变。

代码里的对应方法：`CommonEventHandler#onLivingGetProjectile`。

### 2. 记录界面：只把「是哪一种箭」写进 NBT

箭袋不保存箭矢实物，只在物品 NBT（键 `StoredArrow`）里记下**一支箭的样子**（数量恒为 1）当模板：
`QuiverMenu#clicked` 拦下记录格的点击，只更新这份记录，然后把手上的那叠箭用
`Inventory#placeItemBackInInventory` 原样退回背包；记录格里显示的那一支只是预览，
`ArrowSlot#mayPickup` 恒为 `false`（取不出来），所以不存在凭空刷箭。
好处是箭袋被丢在地上、放进箱子或者在玩家之间交易时，记录都跟着走。

```
QuiverItem        —— 物品本体：NBT 读写、找装备着的箭袋、打开界面
QuiverContainer   —— 1 格「记录格」，直接映射到物品 NBT（数量恒为 1）
QuiverMenu        —— 界面逻辑：记录格 + 玩家 36 格背包（点击只记录，不搬物品）
QuiverScreen      —— 客户端界面绘制（贴图 textures/gui/quiver.png）
CommonEventHandler—— 无限供箭 + 手持右键打开界面
```

### 3. 手持右键为什么不需要网络包

手持物品右键时，客户端一定会走 `MultiPlayerGameMode.useItem` → 服务端 `ServerPlayerGameMode.useItem`，
沿途会触发 `PlayerInteractEvent.RightClickItem`（**双端**都发）；对着方块时还会触发 `RightClickBlock`（服务端）。
两个事件里都判断「服务端 + 手持的是无限箭袋」，取消事件并打开界面即可，无需自制 C2S 包。

### 4. 背饰栏是怎么来的

Curios 的槽位是**按需创建**的：只有当某个物品被加入了对应的物品标签，那个槽才会出现。
所以本模组提供了：

`src/main/resources/data/curios/tags/items/back.json`

```json
{ "replace": false, "values": ["endlessquiver:quiver"] }
```

（1.20.1 的标签目录是 `data/<命名空间>/tags/items/`，注意是复数 `items`。）
不想用背饰栏的话，把这一行改成 `curios:charm` 之类的标签名并新建对应文件即可。

### 5. 射出去的箭为什么捡不回来

供箭逻辑（`onLivingGetProjectile`）采取的是**箭袋优先**：只要箭袋里装了箭，弓和弩拿到的
就是箭袋里那支箭的**副本**（背包里原有几支普通箭也一样会被顶掉，而且不会被消耗）。
所以判据只有一条 —— **这一箭的主人背着装了箭的箭袋** —— 这名玩家射出的每一支箭都是箭袋给的，
全部都不能捡回来。在箭矢实体进入世界时（`EntityJoinLevelEvent`，跳过客户端与 `loadedFromDisk()`）赋值：

```java
// event/CommonEventHandler.java
if (arrow.getOwner() instanceof Player player
        && !QuiverItem.getEquippedArrow(player).isEmpty()) {
    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
}
```

**这里踩过一个坑（v1.0.0 的 bug）**：早先还多写了一层 `!playerCarriesOtherArrows(player)`
（背包里还有别的箭就跳过）。但那层判断和供箭逻辑是矛盾的：只要箭袋里有箭，射出去的就是箭袋的副本，
根本轮不到背包里的箭。于是最常见的场景「身上带着几支普通箭 + 背着装了箭的箭袋」被漏判，
那一箭保持了原版的可拾取，捡回来白赚一支 → 无限刷箭。两处判定现在统一。

赋值时机必须在 `AbstractArrow#setOwner(Entity)` **之后**：那个方法会把 `pickup` 重写成
`ALLOWED`（生存）或 `CREATIVE_ONLY`（创造），早写会被覆盖。而 `EntityJoinLevelEvent` 是实体被
加入世界时触发的，此时构造器（含里面的 `setOwner`）早已执行完，所以我们的赋值能留到最后。

为什么不用「给箭实体打个标记」的写法？因为 `AbstractArrow#getPickupItem()` 是 `protected`，
外部包没法按物品判断箭的种类；而箭袋射出的箭本来就是凭空生成的副本，不需要也不应该回收进背包。
（`Pickup.DISALLOWED` 在 `AbstractArrow#tryPickup` 里走 `default: return false`，生存与创造模式都捡不起来。）

已知的极小偏差：如果某把**模组武器**完全不调用 `Player#getProjectile`（自己造箭实体），
那么背着箭袋时它射出的箭也会变成不可拾取 —— 这类箭本来也不走箭袋供箭流程。

---

## 四、目录结构

```
无限箭袋mod/
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/                Gradle 8.1.1 Wrapper
├── LICENSE                                               MIT
├── cover/                                                封面图（GitHub & MC百科用）
├── libs/curios-forge-5.14.1+1.20.1.jar                  自己放入；仅编译期用，不打包、不入库
├── tools/make_textures.py                               生成两张 PNG 贴图（纯标准库）
├── tools/make_cover.ps1                                 生成封面图（PowerShell + System.Drawing）
└── src/main/
    ├── java/com/example/endlessquiver/
    │   ├── EndlessQuiverMod.java           注册物品与菜单
    │   ├── item/QuiverItem.java            NBT 读写 / 找装备着的箭袋 / 打开界面
    │   ├── inventory/QuiverContainer.java  1 格记录格（映射到 NBT）
    │   ├── inventory/QuiverMenu.java       菜单（点击 = 记录，不搬物品）
    │   ├── client/ClientSetup.java         绑定菜单与界面
    │   ├── client/QuiverScreen.java        界面绘制
    │   └── event/CommonEventHandler.java   无限供箭 + 打开界面
    └── resources/
        ├── META-INF/mods.toml              依赖 forge / minecraft / curios
        ├── data/curios/tags/items/back.json
        ├── data/endlessquiver/recipes/quiver.json                 合成配方
        ├── data/endlessquiver/advancements/recipes/quiver.json    配方书解锁进度
        └── assets/endlessquiver/
            ├── lang/{en_us,zh_cn}.json
            ├── models/item/quiver.json
            └── textures/{item,gui}/quiver.png
```

---

## 五、构建

需要 **JDK 17**。仓库里只带源代码与 Gradle Wrapper，Forge/映射文件在首次构建时联网下载。

```powershell
# 1) 前置：把 Curios 的 jar 放进 libs/（本仓库不收录第三方模组文件）
#    文件名必须与 gradle.properties 里的 curios_version 一致：
#    libs/curios-forge-5.14.1+1.20.1.jar
#    下载页：https://www.curseforge.com/minecraft/mc-mods/curios/files

# 2) 构建
.\gradlew.bat build            # Windows
./gradlew build                # macOS / Linux
```

产物：`build/libs/endlessquiver-1.0.0.jar`，丢进 `.minecraft/mods` 即可（记得同时装 Forge 1.20.1-47.x 与 Curios API）。

> 构建前先让 `gradlew` 找得到 JDK 17：设置环境变量 `JAVA_HOME`，或把 JDK 的 `bin` 加进 `PATH`。
> （Gradle 里的 `org.gradle.java.home` 只决定**Gradle 用哪个 JDK**，替代不了上面这个启动条件。）

构建用 ForgeGradle 6.0.54 + 官方映射（`official` / `1.20.1`），`jar` 任务已 `finalizedBy 'reobfJar'`，
**正式环境必须 reobf**，否则运行时会报 `NoSuchMethodError`。

### 开发环境提示（`runClient` / `runServer`）

`build.gradle` 的 run 配置里特意加了两行：

```groovy
property 'mixin.env.remapRefMap', 'true'
property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
```

开发环境用的是**官方映射**，而 Curios 的 mixin 是按 **SRG 名**写的。不加这两行，
`runClient` / `runServer` 会在加载 Curios 时直接崩：

```
InvalidAccessorException: No candidates were found matching f_19803_:Z
  in net/minecraft/world/entity/Entity for curios.mixins.json:AccessorEntity
```

映射文件要指向 `build/createSrgToMcp/output.srg`（**SRG → official** 方向），
指成 `createMcpToSrg/output.tsrg`（反方向）无效。加上之后 Curios 的 mixin 能过掉
`AccessorEntity`，但 `MixinInventory` 仍会报：

```
@Shadow field f_35978_ was not located in the target class net.minecraft.world.entity.player.Inventory
```

**这是 Curios 自己的 mixin 在开发环境下的已知不兼容**，与箭袋模组无关，正式游戏里不会出现。
所以本模组的运行时验证建议直接用**正式环境的 Forge 服务端**（见下节），不要卡在这里。

### 运行时验证记录（正式环境的 Forge 服务端）

开发环境的 mixin 问题绕不开，所以改用**正式环境**验证：用 Forge 官方安装器
（`forge-1.20.1-47.4.20-installer.jar --installServer`）装了一个真·SRG 命名的服务端，
把 `endlessquiver-1.0.0.jar` 和 `curios-forge-5.14.1+1.20.1.jar` 一起放进 `mods/`：

```
[18:32:50] [modloading-worker-0/INFO] [co.ex.en.EndlessQuiverMod/]: [endlessquiver] 无限箭袋已加载（需要 Curios 作为前置）
[18:32:52] [main/INFO] [to.th.cu.Curios/]: Loaded 10 curio slots
[18:32:55] [Server thread/INFO] [minecraft/DedicatedServer]: Done (2.612s)! For help, type "help"
```

再用 RCON 以控制台身份执行命令，验证物品注册与 NBT 结构：

```
> summon minecraft:item 0 100 0 {Item:{id:"endlessquiver:quiver",Count:1b},...}
Summoned new item.endlessquiver.quiver          ← 物品已注册
> data get entity @e[type=minecraft:item,limit=1]
  Item: {id: "endlessquiver:quiver", Count: 1b}
> data merge entity @e[...] {Item:{tag:{StoredArrow:{id:"minecraft:arrow",Count:1b}}}}
Modified entity data of item.endlessquiver.quiver
> data get entity @e[...] Item.tag.StoredArrow
  {id: "minecraft:arrow", Count: 1b}            ← 记录箭矢的 NBT 结构正确
> reload
Reloading!                                      ← 数据包（含背饰标签）无错误重载
```

**已验证**：模组加载、物品注册、`StoredArrow` NBT 结构、数据包与背饰标签文件。
**未验证（需要真实玩家 / 客户端）**：射箭时是否真的取用箭袋的箭且不消耗、手持右键是否打开界面、
背饰栏是否出现、射出的箭是否真的捡不回来。这几条是靠字节码分析确认的（见上文"实现要点"）。

---

## 六、已知限制

- 箭袋只有 **1 格记录**，所以只能“无限供应一种箭”。记录只能**覆盖**，界面里没有单独的“清空”按钮（想换成别的箭，直接放上去或 `shift + 点击` 另一种箭即可）。
- 记录格只认 `ArrowItem` 的子类。极少数模组箭矢如果不是继承 `ArrowItem`（例如自造弹药的枪械类），记不进去；
  但这类武器通常也不走原版取弹药流程，本来就不在支持范围内。
- 供箭策略是**只要箭袋里有箭就优先用箭袋**：此时背包里的普通箭不会消耗。想要“先消耗背包里的箭”的话，
  把 `CommonEventHandler#onLivingGetProjectile` 里的处理改成先判断
  `event.getProjectileItemStack().isEmpty()` 即可。
- 供应药水箭时，射出的永远是同一个药水效果（这正是“无限药水箭”的预期行为）。
- 装备在背饰栏的箭袋不会渲染成背在背上的模型（本模组只做功能，未加 `ICurioRenderer`）。

---

## 七、封面 / 宣传图

封面**默认只有箭袋本体**（其余只有背景渐变 + 类 MC 噪点 + 一点青色柔光）：没有文字、没有材料行、没有投影、没有边框。
脚本：`tools/make_cover.ps1`（Windows PowerShell 5.1 + `System.Drawing`，不需要 Pillow）。

| 文件 | 尺寸 | 用途 |
| --- | --- | --- |
| `cover/cover_mcmod_1920x1200.png` | 1920×1200（8:5） | **MC百科**模组封面（百科要求 8:5，最小 240×150） |
| `cover/cover_github_1280x640.png` | 1280×640（2:1） | **GitHub** 仓库 Social preview / README 顶部横幅 |
| `cover/cover_mcmod_640x400.png` | 640×400（8:5） | 同上内容的缩小版，传百科小图或当缩略图用 |
| `cover/cover_*_mats.png` | 同上 | 额外带底部合成材料行，加 `-Mats` 才生成 |
| `cover/cover_*_text.png` | 同上 | 带文字版（标题 / 版本 / 卖点 / 材料行 / 页脚），加 `-Text` 才生成 |

```powershell
powershell -ExecutionPolicy Bypass -File tools\make_cover.ps1          # 只有箭袋（默认，3 个尺寸）
powershell -ExecutionPolicy Bypass -File tools\make_cover.ps1 -Mats    # 再额外生成 *_mats.png
powershell -ExecutionPolicy Bypass -File tools\make_cover.ps1 -Text    # 再额外生成 *_mats.png 与 *_text.png
```

带文字版的文案在脚本顶部的 `$TITLE` / `$SUBTITLE` / `$INFO` / `$BULLETS` / `$FOOTER`。

封面里用到的 5 个原版材料图标（下界之星、回响碎片、末影之眼、下界合金锭、下界合金块）来自原版
`client.jar`（版权归 Mojang），**不入库**：需要 `-Mats` / `-Text` 模式时，自己从
`<Forge 缓存>/minecraft_repo/versions/1.20.1/client.jar` 里把
`assets/minecraft/textures/item/{nether_star,echo_shard,ender_eye,netherite_ingot}.png`
与 `assets/minecraft/textures/block/netherite_block.png` 解到 `cover/materials/` 即可。

> 提示：`make_cover.ps1` 含中文，必须保存为 **UTF-8 with BOM** 才能在 PowerShell 5.1 下正确解析。
