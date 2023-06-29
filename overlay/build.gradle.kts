import com.android.utils.appendCapitalized
import de.undercouch.gradle.tasks.download.Download

plugins {
    id ("com.android.application")
    id("de.undercouch.download").version("5.4.0")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.github.xabolcs.grus.cutout.stock"
        namespace = applicationId
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        all {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    signingConfigs {
        all {
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
        create("release") {
            val keystore = System.getenv("RELEASE_KEYSTORE")
            storeFile = if (keystore != null) { File(keystore) } else { null }
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSPHRASE")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSPHRASE")
        }
    }

    flavorDimensions += "notchCurve"
    productFlavors {
        create("stock") {
            dimension = "notchCurve"

            val appDescription = "Stock-like notch"
            ext.properties["appDescription"] = appDescription
            ext.properties["nameSuffix"] = ""

            resValue("string", "label", appDescription)
            resValue("string", "approximation", "M 60 80 L -60 80 L -60 0 L 60 0 L 60 80 Z")
            resValue("string", "cutout", "M -140 0 C -60 0 -74 10 -54 50 C -43 71 -21 80 0 80 C 21 80 43 71 54 50 C 74 10 60 0 140 0 V -1 H -140 Z")

        }
        create("smoothStock") {
            dimension = "notchCurve"
            applicationIdSuffix = ".smooth"

            val appDescription = "Stock-like notch with smoothened curves"
            ext["appDescription"] = appDescription
            ext["nameSuffix"] = " - smoothened"

            resValue("string", "label", appDescription)
            resValue("string", "approximation", "M 64 80 L -64 80 L -64 0 L 64 0 L 64 80 Z")
            resValue("string", "cutout", "M -145 0 C -100 0 -87 4 -61 47 C -49 67 -28 80 0 80 C 28 80 49 67 61 47 C 87 4 100 0 145 0 L 145 -1 L -145 -1 Z")
        }
    }
}

androidComponents {
    onVariants {
        afterEvaluate {
            task("packageMagisk${it.name.capitalize()}", Zip::class) {
                val zipTask = it
                val fileName = "Grus${"".appendCapitalized(zipTask.flavorName.toString())}Cutout"

                var appDescription = ""
                var nameSuffix = ""
                when(zipTask.flavorName){
                    "stock" -> appDescription = "Stock-like notch"
                    "smoothStock" -> {
                        appDescription = "Stock-like notch with smoothened curves"
                        nameSuffix = " - smoothened"
                    }
                }


                dependsOn(tasks["package${it.name.capitalize()}"])

                destinationDirectory.set(buildDir.resolve("outputs/magisk/${it.name}"))
                archiveBaseName.set("${fileName}Magisk")

                from(tasks["package${it.name.capitalize()}"]) {
                    into("system/product/overlay/DisplayCutoutEmulation${fileName.removeSuffix("Cutout")}Overlay")
                    include("*.apk")
                    rename { "${fileName}.apk" }
                }
                from(project.file("src/main/magisk/")) {
                    exclude("/module.prop")
                }
                from(project.file("src/main/magisk/module.prop")) {
                    expand(
                        "applicationId" to it.applicationId.get(),
                        "versionCode" to it.outputs.first().versionCode.get(),
                        "versionName" to it.outputs.first().versionName.get(),
                        "nameSuffix" to nameSuffix,
                        "appDescription" to appDescription
                    )
                }

                tasks["assemble${it.name.capitalize()}"].dependsOn(this)
            }
        }
    }
}

tasks.register("downloadModuleInstaller", Download::class) {
    dest("${project.projectDir}/src/main/magisk/META-INF/com/google/android/update-binary")
    src("https://raw.githubusercontent.com/chenxiaolong/BCR/master/app/magisk/update-binary")
    onlyIfModified(true)
}
