plugins {
    id("fpgradle-minecraft") version("0.10.1")
}

group = "mega"

minecraft_fp {
    mod {
        modid = "fluidlogged"
        name = "FluidLogged"
        rootPkg = "$group.fluidlogged"
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
}

repositories {
    exclusive(mavenpattern(), "com.falsepattern")
    cursemavenEX()
    modrinthEX()
}

dependencies {
    implementationSplit("com.falsepattern:falsepatternlib-mc1.7.10:1.5.9")
    implementationSplit("com.falsepattern:chunkapi-mc1.7.10:0.6.1")
    compileOnly(deobfCurse("buildcraft-61811:4055732"))
    devOnlyNonPublishable(deobfCurse("thermal-expansion-69163:2388758"))
    devOnlyNonPublishable(deobfCurse("thermal-foundation-222880:2388752"))
    compileOnly(deobfCurse("cofhcore-69162:2388750"))

    runtimeOnlyNonPublishable(deobfModrinth("baubles-expanded:2.1.4"))
}
