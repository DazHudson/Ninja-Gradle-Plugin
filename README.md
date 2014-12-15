Gradle Superdev Plugin for Ninja.
=================================


contextPath = Jetty context path
port = Jetty Port
scanDirs = Directories to watch
watchResources = If files in resources change, restart
hotswapEnabled = true if hot class loading required. 
debugPort = JVM debug port to listen on
suspend = If true await for the debugger to attach


task superdev(type: ninja.plgun.gradle.SuperDevTask,dependsOn: ['compileJava','processResources']) {
    
    contextPath = '/'
    port = 8080
    scanDirs = sourceSets.main.runtimeClasspath.getFiles()
    watchResources = true
    hotswapEnable = false
    debugEnabled = true
    debugPort = 5006
    //suspend = true
}

task superhot(type: ninja.plugin.gradle.SuperDevTask,dependsOn: ['compileJava','processResources']) {
    
    // Only need Spring Loaded in hot mode
    dependencies {
           compile "org.springframework:springloaded:1.2.0.RELEASE" 
    }
    
    contextPath = '/'
    port = 8080
    scanDirs = sourceSets.main.runtimeClasspath.getFiles()
    watchResources = false
    hotswapEnable = true
    debugEnabled = true
    debugPort = 5006
}

When using IDE's superdev may need a kick as the IDE caches the compiled classes, using the gradle compileJava works.

Pressing any key in superdev will re-load Ninja.







