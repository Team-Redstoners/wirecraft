plugins {
	`java-library`
	id("io.github.goooler.shadow") version "8.1.7"
}

repositories {
	mavenCentral()
	maven {
		name = "papermc"
		url = uri("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
	implementation("com.github.kurbatov:firmata4j:2.3.8")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
	jar {
		manifest {
			attributes["paperweight-mappings-namespace"] = "mojang"
		}
	}
	shadowJar {
		archiveFileName.set("Wirecraft-SHADED-${project.version}.jar")
		manifest {
			attributes["paperweight-mappings-namespace"] = "mojang"
		}
	}
	build {
		dependsOn(shadowJar)
	}
}
