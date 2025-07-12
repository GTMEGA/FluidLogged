plugins {
    id("com.falsepattern.fpgradle-mc") version "0.19.6"
}

group = "mega"

minecraft_fp {
    mod {
        modid = "fluidlogged"
        name = "FluidLogged"
        rootPkg = "$group.fluidlogged"
    }

    api {
        packages = listOf("api")
    }

    mixin {
        pkg = "internal.mixin.mixins"
        pluginClass = "internal.mixin.plugin.MixinPlugin"
    }

    core {
        coreModClass = "internal.core.CoreLoadingPlugin"
        accessTransformerFile = "fluidlogged_at.cfg"
    }

    tokens {
        tokenClass = "Tags"
    }

    publish {
        changelog = "https://github.com/GTMEGA/FluidLogged/releases/tag/$version"
        maven {
            repoName = "mega"
            repoUrl = "https://mvn.falsepattern.com/gtmega_releases"
        }
    }
}

repositories {
    exclusive(mavenpattern(), "com.falsepattern")
    cursemavenEX()
}

dependencies {
    implementationSplit("com.falsepattern:falsepatternlib-mc1.7.10:1.7.0")
    implementationSplit("com.falsepattern:chunkapi-mc1.7.10:0.6.4")
    compileOnly(deobfCurse("cofhcore-69162:2388750"))
}
