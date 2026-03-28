plugins {
    id("java")
    id("maven-publish")
}

group = "com.iso2t"
version = "1.1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.google.code.gson:gson:2.13.2")

    implementation("com.fasterxml.jackson.core:jackson-core:2.21.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "iso2t"
            url = uri("https://maven.iso2t.com/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "com.iso2t"
            artifactId = "ConfigManager"
            version = "1.1.1"
            from(components["java"])
        }
    }
}