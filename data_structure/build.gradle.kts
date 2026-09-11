plugins {
    id("java")
    kotlin("jvm") version "1.8.21"
}

group = "com.nj"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

//java {
//    toolchain {
//        implementation = JvmImplementation.J11
//    }
//}
