grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.war.file = "target/${appName}.war"
// Remove the git folder before the war is bundled
grails.war.resources = { stagingDir ->
    echo message: "StagingDir: $stagingDir"
    delete(dir:"${stagingDir}/git/")
}
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve true // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility
    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo "http://repo.grails.org/grails/repo/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        runtime 'mysql:mysql-connector-java:5.1.33'
        runtime 'org.postgresql:postgresql:9.3-1102-jdbc41'
        // runtime 'postgresql:postgresql:9.1-901.jdbc4'
        // compile "org.eclipse.jgit:org.eclipse.jgit:3.1.0.201310021548-r"
        // compile "net.sf.opencsv:opencsv:2.3"
        // compile "jcifs:jcifs:1.3.17"
        compile 'org.apache.directory.api:api-all:1.0.0-M24'
        compile 'org.apache-extras.camel-extra:camel-jcifs:2.13.2'
        compile "javax.mail:mail:1.4"
        compile "com.xlson.groovycsv:groovycsv:1.0"
        runtime 'org.jruby:jruby:1.7.15'
        runtime 'org.python:jython-standalone:2.5.3'
    }

    plugins {
        runtime ":hibernate:$grailsVersion"
        runtime ":jquery:1.8.0"
        runtime ":resources:1.1.6"

        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.4"

        build ":tomcat:$grailsVersion"

        runtime ":database-migration:1.1"

        compile ':cache:1.0.0'
        compile ':spring-security-core:1.2.7.3'
        compile ":spring-security-acl:1.1.1"
        compile ':spring-security-cas-usf:1.2.1'
        compile ':grails-cas-rest-client:0.3.1'
        compile ":quartz2:2.1.6.2"
        compile ":rest:0.7"
        compile ":jgit:1.0.1"
        compile ":csv:0.3.1"
        runtime ":cors:1.1.6"
    }
}
