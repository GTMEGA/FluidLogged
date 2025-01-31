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
        accessTransformerFile = "fluidlogged_at.cfg"
    }

    tokens {
        tokenClass = "Tags"
    }
}

repositories {
    exclusive(mavenpattern(), "com.falsepattern")
}

dependencies {
    implementationSplit("com.falsepattern:falsepatternlib-mc1.7.10:1.5.9")
    implementationSplit("com.falsepattern:chunkapi-mc1.7.10:0.6.1")
}
