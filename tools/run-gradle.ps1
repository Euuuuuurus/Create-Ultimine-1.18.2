# Helper to run Gradle with the correct JDK 17 and a workspace-local GRADLE_USER_HOME
# (the DSH sandbox only allows writes inside the workspace).
#
# DSH-environment workarounds (see README):
#  - JDK 17.0.8 (tools/jdk17): the environment breaks Files.isWritable() (always false), which
#    makes jdk.zipfs open every zip read-only and breaks ForgeGradle 5.1's AccessTransformer step.
#    The patched ZipFileSystem (tools/zipfs-patch, built by tools/PatchZipfs.java) forces
#    readOnly=false; JAVA_TOOL_OPTIONS propagates --patch-module to the Gradle daemon AND to every
#    forked java child (including FG's internal JarExec invoked during configuration).
#  - -Dnet.minecraftforge.gradle.check.certs=false: the sandbox's Windows Schannel TLS stack cannot
#    validate maven.minecraftforge.net, so ForgeGradle aborts while applying its plugin. Only needed
#    here; on a normal machine remove this flag (and the whole JAVA_TOOL_OPTIONS line).
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Args
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'D:\agent\createUltimine\tools\jdk17\jdk-17.0.8+7'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = 'D:\agent\createUltimine\.gradle-home'
$env:JAVA_TOOL_OPTIONS = "--patch-module=jdk.zipfs=D:/agent/createUltimine/tools/zipfs-patch -Dnet.minecraftforge.gradle.check.certs=false"
& (Join-Path $PSScriptRoot '..\gradlew.bat') @Args
exit $LASTEXITCODE
