# Create Ultimine — Minecraft 1.18.2 Forge 移植版

将 [ChAoSUnItY/Create-Ultimine](https://github.com/ChAoSUnItY/Create-Ultimine/)（一个给 Create 模组增加 FTB Ultimine 联动功能的插件）
从上游 1.20-forge / 1.21-neoforge 分支**移植到 Minecraft 1.18.2 Forge** 的工程。

上游仓库历史上从未发布过真正的 1.18.2 功能版（1.18.2 提交只是 Kotlin 模板），因此本移植以功能最接近的
`1.20-forge`（v1.3.1）为基线，并参考 `1.21-neoforge`（v1.3.3）的实现，整体适配到 1.18.2。

## 功能

- 按住 FTB Ultimine 键 + 右键：
  - 手持“合金”（如安山合金）等可手动应用物品时，对缓存的所有方块统一执行 Create 的手动应用配方
    （去皮原木、机壳、砂纸打磨等）。
  - 手持 Create 扳手（或 `#forge:wrench` 标签物品）时，对所有缓存方块统一执行扳手交互（旋转、拆除等）。
- 服务器配置（SNBT）：`features.right_click_alloy` / `features.right_click_wrench`，
  存放于世界目录 `serverconfig/createultimine-server.snbt`。

## 版本矩阵（已锁定并验证）

| 组件 | 版本 | 说明 |
|---|---|---|
| Minecraft | 1.18.2 | |
| Forge | 40.2.21 | 满足 Create 0.5.1.i 要求的 `[40.2.4,)` |
| Java | 17 | 1.18.2 必须用 Java 17 |
| Gradle | 7.4.2 | 配合 ForgeGradle 5.1 |
| ForgeGradle | 5.1.+ | `net.minecraftforge.gradle` |
| Kotlin | 1.8.21 | 与 KFF 3.12.0 内置 Kotlin 一致（`kfflib` 捆绑 1.8.21） |
| Kotlin for Forge | 3.12.0 (`thedarkcolour`) | 支持 1.18.2 的最新 3.x；`modLoader="kotlinforforge"` |
| Create | 0.5.1.i（1.18.2） | 经 `local-maven/` 以模块依赖引入（见下） |
| Registrate | MC1.18.2-1.1.3 | Create 的注册表库，经 `local-maven/` 引入 |
| FTB Ultimine | 1802.3.4-build.93 | 1.18.2 最终版 |
| FTB Library | 1802.3.6-build.115 | 1.18.2 的 `snbt.config` API |
| Architectury | 4.1.36（Forge） | FTB Ultimine 1.18.2 的运行时依赖 |

## 关键移植点（相比上游 1.20-forge）

1. **Mixin 目标签名不同**。上游 1.20 注入的 `FTBUltimine.blockRightClick(Player, InteractionHand, BlockPos,
   Direction, FTBUltiminePlayerData)` 在 1.18.2 是 `(Player, InteractionHand, BlockPos, Direction)`，
   且局部变量只有 `serverPlayer / result / data / shapeContext`（没有 `blockHitResult`）。注入点仍为
   `FTBUltiminePlayerData.updateBlocks(...)` 的 `INVOKE_ASSIGN`，`shapeContext` 赋值后立即注入。
2. **FTB Ultimine 1.18.2 没有** `CooldownTracker`、`addPendingXPCost`（2001.x 才引入），
   因此移植版不包含 1.20 版中的冷却/经验值扣费逻辑，仅在成功时 `player.swing(hand)` 并 `interruptFalse()` 取消原事件。
3. **`FTBUltiminePlayerData` 公开字段**：1.18.2 用 `data.cachedBlocks`、`data.pressed`（字段），
   而不是 1.20 的 `cachedPositions()` / `isPressed()` 方法。
4. **配置 API**：1.18.2 FTB Library 用 `SNBTConfig.getGroup / getBoolean`（1.20 是 `addGroup / addBoolean`）。
5. **Create 0.5.1 API 差异**：`AllTags.AllItemTags.WRENCH` 暴露 `tag` 字段（`ItemStack.is(tag)`），
   没有 1.20 的 `matches(ItemStack)` 方法。`ManualApplicationRecipe.manualApplicationRecipesApplyInWorld(PlayerInteractEvent.RightClickBlock)`
   在 0.5.1 中已存在且签名一致。
6. **主类事件**：改用原生 Forge 事件（`ServerStartingEvent`）加载服务端配置，不再依赖 Architectury 事件
   （去掉了 1.20 版的玩家加入时 `SyncConfigFromServerPacket`，该包在 1.18.2 的 FTB Ultimine 中不存在，且 1.18.2 无对应客户端消费方）。
7. **Mixin 注册方式**：1.18.2 Forge 没有 NeoForge 的 `[[mixins]]` 段。生产环境用 jar 清单的
   `MixinConfigs` 属性（`build.gradle` 的 `jar { manifest { ... } }`）；开发环境从 `build/resources/main`
   加载、没有清单，因此 `build.gradle` 里额外生成了一个 dev 专用 `META-INF/MANIFEST.MF`
   （`devManifest` 任务），主类构造器里也调用 `Mixins.addConfiguration(...)` 作为兜底。

## 依赖接入方式（重要）

1.18.2 Forge 的生产运行时成员名是 **SRG**（`m_135828_`、`f_143509_` …），而 ForgeGradle 5.1 的
`official` 映射开发环境用的是 **Mojang 官方名**（`isAllowedInResourceLocation`、`OPERATING_SYSTEM` …）。
第三方模组（Create / FTB / Architectury）都是按 SRG 发布的，直接丢进开发环境会有两类崩溃：

- 它们的 Mixin 找不到目标（`could not find any targets matching 'm_135828_(C)Z'`）；
- 它们的字节码引用 SRG 字段（`NoSuchFieldError: f_76278_`）。

因此**所有模组依赖都必须是模块依赖并经过 `fg.deobf(...)` 重映射**，且开发运行时需要开启 refmap 重映射：

```groovy
implementation fg.deobf("dev.ftb.mods:ftb-ultimine-forge:...")   // 且 exclude 掉 ftb-ranks
implementation fg.deobf("com.simibubi.create:create:1.18.2-0.5.1.i")
implementation fg.deobf("com.tterrag.registrate:Registrate:MC1.18.2-1.1.3")
```

```groovy
// runClient / runServer 里
property 'mixin.env.remapRefMap', 'true'
property 'mixin.env.refMapRemappingFile', file('build/createSrgToMcp/output.srg').absolutePath
```

注意：`fg.deobf(files(...))` 对**文件依赖不生效**（FG 不会重映射它），必须走模块依赖，所以：

- `local-maven/` 是本地 Maven 仓库镜像，存放 Create 与 Registrate（公开仓库没有 Create 的 Maven 坐标）：
  - `com/simibubi/create/create/1.18.2-0.5.1.i/`
  - `com/tterrag/registrate/Registrate/MC1.18.2-1.1.3/`
  - 其中 Create jar 由 `tools/StripJarJarEntry.java` 剥离了内嵌的 Registrate（保留 Flywheel）：
    内嵌副本是 SRG 的、会在运行时 `NoSuchFieldError`，而 Registrate 又以模块形式引入，
    两份同时存在会报 `module.ResolutionException: Modules Registrate.MC1._18._2 and Registrate export package ...`。
- FTB 依赖从 `maven.ftb.dev` / `maven.architectury.dev` 解析，并显式 `exclude` 掉 `ftb-ranks-forge`：
  ftb-ranks 会把 ftb-library 顶到 1802.3.8、architectury 顶到 4.4.54，破坏已锁定的版本组合。

## 准备本地依赖（clone 后首次构建必读）

本仓库**只存放源码，不包含任何第三方模组二进制**（与上游分支惯例一致）。构建前需自行准备两个
Create 生态的 jar，放进 `local-maven/` 对应目录（`.pom` 文件仓库中已提供，无需创建）：

```
local-maven/
├── com/simibubi/create/create/1.18.2-0.5.1.i/
│   ├── create-1.18.2-0.5.1.i.jar          <- 需要你放入
│   └── create-1.18.2-0.5.1.i.pom          (仓库已提供)
└── com/tterrag/registrate/Registrate/MC1.18.2-1.1.3/
    ├── Registrate-MC1.18.2-1.1.3.jar      <- 需要你放入
    └── Registrate-MC1.18.2-1.1.3.pom      (仓库已提供)
```

1. **Create 0.5.1.i**：从 [CurseForge](https://www.curseforge.com/minecraft/mc-mods/create)
   （project id `328085`，file id `5797603`，文件名 `create-1.18.2-0.5.1.i.jar`）或
   [Modrinth](https://modrinth.com/mod/create) 下载，放入上面的 Create 目录。
2. **Registrate**：无需单独下载，它内嵌在 Create jar 中。解压 Create jar，取出
   `META-INF/jarjar/Registrate-MC1.18.2-1.1.3.jar`，放入上面的 Registrate 目录。

> 为什么不像上游那样直接用 Maven？因为公开 Maven 没有 Create 的可用坐标
> （CurseForge Maven 与 cursemaven 均已实测不可访问），Registrate 也没有单独发布。
> 而这两者必须经 `fg.deobf` 重映射才能在开发环境正常使用，所以只能以本地 Maven 镜像提供。
>
> 缺少 jar 时 Gradle 会在配置阶段直接报错并打印上述步骤，不会给出难以理解的依赖解析失败。
>
> FTB Ultimine / FTB Library / Architectury / Kotlin for Forge 会自动从各自 Maven 仓库下载，无需手动准备。

## 运行方式

```powershell
# 首次构建（会自动下载 Gradle 7.4.2、Minecraft/Forge、依赖，较慢）
.\tools\run-gradle.ps1 build

# 启动客户端
.\tools\run-gradle.ps1 runClient

# 启动服务端（无头验证用；首次需在 run/server/eula.txt 写入 eula=true）
.\tools\run-gradle.ps1 runServer
```

> `tools/run-gradle.ps1` 固定了本机 JDK 17 路径（`tools/jdk17/jdk-17.0.8+7`）
> 与工作区内的 Gradle 缓存目录（`.gradle-home`）。在其他机器上把 JDK 17 设为 `JAVA_HOME` 后直接运行
> `gradlew.bat`，并把 `gradle.properties` 里的 `org.gradle.user.home` 改为你的路径即可。

### 本机（DSH 沙箱）环境 workaround

`tools/run-gradle.ps1` 与 `gradle.properties` 里做了三件**仅限本沙箱**的处理，普通机器上都不需要：

1. **`JAVA_TOOL_OPTIONS=--patch-module=jdk.zipfs=<补丁目录>`**：本沙箱中 `Files.isWritable()` 恒为
   `false`（Windows AccessCheck 探针被环境拦截），导致 JDK 的 `jdk.zipfs` 把所有 zip 按只读打开，
   ForgeGradle 5.1 的 AccessTransformer 步骤必然崩溃（`ReadOnlyFileSystemException`）。
   补丁由 `tools/PatchZipfs.java` 生成：把 `ZipFileSystem` 构造器里的 `iload 5` 改成 `iconst_1`
   （2 字节，栈形状与字节长度都不变，因此 StackMapTable 仍然有效），使 `readOnly` 恒为 `false`。
   **普通机器请删除 `tools/run-gradle.ps1` 里的 `JAVA_TOOL_OPTIONS` 行。**
2. **`-Djava.io.tmpdir=<工作区>/tmp`**（`org.gradle.jvmargs` 与 run 配置各一处）：Kotlin 编译器要在
   `java.io.tmpdir` 写 `.alive` 标记文件、JNA 要解压 `jnidispatch.dll`，而系统 Temp 在本沙箱不可写。
   **普通机器删除即可。**
3. **`kotlin.compiler.execution.strategy=in-process`**：避开独立 Kotlin daemon 的临时文件。可选。

## 打包产物

- `build/libs/createultimine-1.18.2-forge-<version>.jar`（当前为 `1.3.1-1.18.2`）
- 玩家实际使用时需在 `mods/` 放置：Create 0.5.1.i、FTB Ultimine 1802.3.4、FTB Library 1802.3.6、
  Architectury 4.1.36、Kotlin for Forge 3.12.0（Registrate/Flywheel 由 Create 的 jar-in-jar 自带）。
  玩家环境用的是这些模组的**正式发布 jar**，无需上面 `local-maven/` 的任何处理。

## 已验证内容（runServer 实测）

- Kotlin for Forge 正常加载：`Creating KotlinModContainer instance for io.github.chaosunity.createultimine.CreateUltimine`
- Mixin 配置注册：`Registering mixin config: mixins.createultimine.json`
- **Mixin 成功注入**：`Mixing UltimineMixin from mixins.createultimine.json into dev.ftb.mods.ftbultimine.FTBUltimine`
- 服务端启动完成：`Done (2.555s)! For help, type "help"`，全程无 FATAL / 无 Mixin 注入失败
- 配置文件生成：`run/server/world/serverconfig/createultimine-server.snbt`（含两个开关与注释）

## 目录结构

```
build.gradle / gradle.properties / settings.gradle   # 构建配置
gradle/wrapper/                                      # Gradle 7.4.2 wrapper
src/main/kotlin/.../CreateUltimine.kt                # @Mod 入口（Kotlin）
src/main/kotlin/.../RightClickHandlers.kt            # 合金/扳手批量右键逻辑
src/main/kotlin/.../config/CreateUltimineServerConfig.kt
src/main/java/.../mixin/UltimineMixin.java           # 注入 FTBUltimine.blockRightClick
src/main/resources/META-INF/mods.toml                # modLoader="kotlinforforge"
src/main/resources/mixins.createultimine.json
src/main/resources/pack.mcmeta / logo.png
libs/                                                # Flywheel(compileOnly) 等本地 jar
local-maven/                                         # Create / Registrate 的本地 Maven 镜像
tools/                                               # 构建脚本与沙箱 workaround 工具
```
